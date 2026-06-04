package dev.lucasxie.learning.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.lucasxie.learning.auth.security.AuthenticatedUser;
import dev.lucasxie.learning.common.exception.BusinessException;
import dev.lucasxie.learning.common.exception.ErrorCode;
import dev.lucasxie.learning.course.Course;
import dev.lucasxie.learning.course.CourseRepository;
import dev.lucasxie.learning.course.CourseStatus;
import dev.lucasxie.learning.file.FileAccessProperties;
import dev.lucasxie.learning.file.FileAsset;
import dev.lucasxie.learning.file.FileAssetRepository;
import dev.lucasxie.learning.file.FileAssetType;
import dev.lucasxie.learning.file.StorageProvider;
import dev.lucasxie.learning.lesson.Lesson;
import dev.lucasxie.learning.lesson.LessonRepository;
import dev.lucasxie.learning.lesson.LessonStatus;
import dev.lucasxie.learning.storage.StorageDownloadResult;
import dev.lucasxie.learning.storage.StorageProperties;
import dev.lucasxie.learning.storage.StorageService;
import dev.lucasxie.learning.storage.StorageUploadCommand;
import dev.lucasxie.learning.storage.StorageUploadResult;
import dev.lucasxie.learning.user.UserStatus;

class FileAssetServiceImplTest {

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void uploadStoresFileAssetMetadataAndCallsStorage() {
		TestFixture fixture = new TestFixture();
		authenticate(teacher(10L));
		when(fixture.fileAssetRepository.saveAndFlush(any(FileAsset.class))).thenAnswer(invocation -> {
			FileAsset fileAsset = invocation.getArgument(0);
			fileAsset.setId(1L);
			return fileAsset;
		});
		when(fixture.storageService.upload(any(StorageUploadCommand.class))).thenReturn(new StorageUploadResult(
			"database/key",
			"http://localhost:8080/api/v1/files/1/content",
			"image/png",
			4L,
			"checksum",
			StorageProvider.DATABASE
		));

		var response = fixture.service.upload(
			multipart("cover.png", "image/png", new byte[] {1, 2, 3, 4}),
			FileAssetType.COURSE_COVER
		);

		ArgumentCaptor<FileAsset> fileAssetCaptor = ArgumentCaptor.forClass(FileAsset.class);
		verify(fixture.fileAssetRepository).saveAndFlush(fileAssetCaptor.capture());
		FileAsset savedFileAsset = fileAssetCaptor.getValue();
		assertThat(savedFileAsset.getOriginalName()).isEqualTo("cover.png");
		assertThat(savedFileAsset.getContentType()).isEqualTo("image/png");
		assertThat(savedFileAsset.getSizeBytes()).isEqualTo(4L);
		assertThat(savedFileAsset.getAssetType()).isEqualTo(FileAssetType.COURSE_COVER);
		assertThat(savedFileAsset.getOwnerId()).isEqualTo(10L);

		ArgumentCaptor<StorageUploadCommand> commandCaptor = ArgumentCaptor.forClass(StorageUploadCommand.class);
		verify(fixture.storageService).upload(commandCaptor.capture());
		assertThat(commandCaptor.getValue().fileAssetId()).isEqualTo(1L);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.url()).isEqualTo("http://localhost:8080/api/v1/files/1/content");
		assertThat(response.storageProvider()).isEqualTo(StorageProvider.DATABASE);
	}

	@Test
	void uploadRejectsEmptyFile() {
		TestFixture fixture = new TestFixture();
		authenticate(teacher(10L));

		assertThatThrownBy(() -> fixture.service.upload(
			multipart("cover.png", "image/png", new byte[0]),
			FileAssetType.COURSE_COVER
		))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.FILE_EMPTY.name());
		verifyNoInteractions(fixture.storageService);
	}

	@Test
	void uploadRejectsOversizedFile() {
		TestFixture fixture = new TestFixture();
		fixture.storageProperties.setMaxFileSizeMb(0);
		authenticate(teacher(10L));

		assertThatThrownBy(() -> fixture.service.upload(
			multipart("cover.png", "image/png", new byte[] {1}),
			FileAssetType.COURSE_COVER
		))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.FILE_TOO_LARGE.name());
		verifyNoInteractions(fixture.storageService);
	}

	@Test
	void uploadRejectsUnsupportedCourseCoverContentType() {
		TestFixture fixture = new TestFixture();
		authenticate(teacher(10L));

		assertThatThrownBy(() -> fixture.service.upload(
			multipart("cover.pdf", "application/pdf", new byte[] {1}),
			FileAssetType.COURSE_COVER
		))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.FILE_UNSUPPORTED_TYPE.name());
		verifyNoInteractions(fixture.storageService);
	}

	@Test
	void studentCannotUploadFiles() {
		TestFixture fixture = new TestFixture();
		authenticate(student(30L));

		assertThatThrownBy(() -> fixture.service.upload(
			multipart("cover.png", "image/png", new byte[] {1}),
			FileAssetType.COURSE_COVER
		))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.FILE_ACCESS_DENIED.name());
	}

	@Test
	void adminCanDeleteAnyFile() {
		TestFixture fixture = new TestFixture();
		FileAsset fileAsset = fileAsset(1L, 20L, FileAssetType.COURSE_COVER);
		when(fixture.fileAssetRepository.findById(1L)).thenReturn(Optional.of(fileAsset));
		authenticate(admin(1L));

		fixture.service.deleteFile(1L);

		verify(fixture.storageService).delete(fileAsset.getStorageKey());
		verify(fixture.fileAssetRepository).delete(fileAsset);
	}

	@Test
	void teacherCanDeleteOwnedFile() {
		TestFixture fixture = new TestFixture();
		FileAsset fileAsset = fileAsset(1L, 10L, FileAssetType.COURSE_COVER);
		when(fixture.fileAssetRepository.findById(1L)).thenReturn(Optional.of(fileAsset));
		authenticate(teacher(10L));

		fixture.service.deleteFile(1L);

		verify(fixture.storageService).delete(fileAsset.getStorageKey());
		verify(fixture.fileAssetRepository).delete(fileAsset);
	}

	@Test
	void teacherCannotDeleteAnotherUsersFile() {
		TestFixture fixture = new TestFixture();
		FileAsset fileAsset = fileAsset(1L, 99L, FileAssetType.COURSE_COVER);
		when(fixture.fileAssetRepository.findById(1L)).thenReturn(Optional.of(fileAsset));
		authenticate(teacher(10L));

		assertThatThrownBy(() -> fixture.service.deleteFile(1L))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.FILE_ACCESS_DENIED.name());
		verifyNoInteractions(fixture.storageService);
	}

	@Test
	void teacherCanBindCourseCoverToOwnedCourse() {
		TestFixture fixture = new TestFixture();
		Course course = course(1L, 10L, CourseStatus.DRAFT);
		FileAsset fileAsset = fileAsset(2L, 10L, FileAssetType.COURSE_COVER);
		when(fixture.courseRepository.findById(1L)).thenReturn(Optional.of(course));
		when(fixture.fileAssetRepository.findById(2L)).thenReturn(Optional.of(fileAsset));
		authenticate(teacher(10L));

		var response = fixture.service.bindCourseCover(1L, 2L);

		assertThat(course.getCoverFileId()).isEqualTo(2L);
		assertThat(fileAsset.getRelatedType()).isEqualTo("COURSE");
		assertThat(fileAsset.getRelatedId()).isEqualTo(1L);
		assertThat(response.relatedType()).isEqualTo("COURSE");
	}

	@Test
	void teacherCannotBindCourseCoverToAnotherTeachersCourse() {
		TestFixture fixture = new TestFixture();
		Course course = course(1L, 99L, CourseStatus.DRAFT);
		when(fixture.courseRepository.findById(1L)).thenReturn(Optional.of(course));
		authenticate(teacher(10L));

		assertThatThrownBy(() -> fixture.service.bindCourseCover(1L, 2L))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.FILE_ACCESS_DENIED.name());
	}

	@Test
	void bindingLessonAudioUpdatesLessonAudioFileId() {
		TestFixture fixture = new TestFixture();
		Course course = course(1L, 10L, CourseStatus.DRAFT);
		Lesson lesson = lesson(3L, 1L, LessonStatus.DRAFT);
		FileAsset fileAsset = fileAsset(4L, 10L, FileAssetType.LESSON_AUDIO);
		when(fixture.lessonRepository.findById(3L)).thenReturn(Optional.of(lesson));
		when(fixture.courseRepository.findById(1L)).thenReturn(Optional.of(course));
		when(fixture.fileAssetRepository.findById(4L)).thenReturn(Optional.of(fileAsset));
		authenticate(teacher(10L));

		fixture.service.bindLessonAudio(3L, 4L);

		assertThat(lesson.getAudioFileId()).isEqualTo(4L);
		assertThat(fileAsset.getRelatedType()).isEqualTo("LESSON");
		assertThat(fileAsset.getRelatedId()).isEqualTo(3L);
	}

	@Test
	void bindingRejectsMismatchedFileType() {
		TestFixture fixture = new TestFixture();
		Course course = course(1L, 10L, CourseStatus.DRAFT);
		FileAsset fileAsset = fileAsset(2L, 10L, FileAssetType.LESSON_AUDIO);
		when(fixture.courseRepository.findById(1L)).thenReturn(Optional.of(course));
		when(fixture.fileAssetRepository.findById(2L)).thenReturn(Optional.of(fileAsset));
		authenticate(teacher(10L));

		assertThatThrownBy(() -> fixture.service.bindCourseCover(1L, 2L))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.FILE_BINDING_INVALID.name());
	}

	@Test
	void downloadReturnsStoredContent() {
		TestFixture fixture = new TestFixture();
		FileAsset fileAsset = fileAsset(1L, 10L, FileAssetType.COURSE_COVER);
		when(fixture.fileAssetRepository.findById(1L)).thenReturn(Optional.of(fileAsset));
		when(fixture.storageService.download(fileAsset.getStorageKey())).thenReturn(new StorageDownloadResult(
			new byte[] {1, 2, 3},
			"image/png",
			3L,
			"cover.png"
		));
		authenticate(teacher(10L));

		FileDownload download = fixture.service.downloadFileContent(1L);

		assertThat(download.content()).isEqualTo(new byte[] {1, 2, 3});
		assertThat(download.contentType()).isEqualTo("image/png");
		assertThat(download.sizeBytes()).isEqualTo(3L);
		assertThat(download.originalName()).isEqualTo("cover.png");
	}

	@Test
	void createAccessUrlReturnsSignedPublicContentUrl() {
		TestFixture fixture = new TestFixture();
		FileAsset fileAsset = fileAsset(1L, 10L, FileAssetType.COURSE_COVER);
		when(fixture.fileAssetRepository.findById(1L)).thenReturn(Optional.of(fileAsset));
		authenticate(teacher(10L));

		var response = fixture.service.createAccessUrl(1L);

		assertThat(response.url()).startsWith("http://localhost:8080/api/v1/files/1/public-content?token=");
		assertThat(response.expiresAt()).isNotNull();
	}

	@Test
	void publicDownloadReturnsStoredContentForValidToken() {
		TestFixture fixture = new TestFixture();
		FileAsset fileAsset = fileAsset(1L, 10L, FileAssetType.COURSE_COVER);
		String token = fixture.fileAccessTokenService.generateToken(1L, java.time.Instant.now().plusSeconds(60));
		when(fixture.fileAssetRepository.findById(1L)).thenReturn(Optional.of(fileAsset));
		when(fixture.storageService.download(fileAsset.getStorageKey())).thenReturn(new StorageDownloadResult(
			new byte[] {9, 8},
			"image/png",
			2L,
			"cover.png"
		));

		FileDownload download = fixture.service.downloadPublicFileContent(1L, token);

		assertThat(download.content()).isEqualTo(new byte[] {9, 8});
		assertThat(download.contentType()).isEqualTo("image/png");
	}

	@Test
	void publicDownloadRejectsInvalidToken() {
		TestFixture fixture = new TestFixture();

		assertThatThrownBy(() -> fixture.service.downloadPublicFileContent(1L, "bad-token"))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.FILE_ACCESS_TOKEN_INVALID.name());
		verifyNoInteractions(fixture.storageService);
	}

	@Test
	void unbindCourseCoverClearsCourseAndRelatedFileMetadata() {
		TestFixture fixture = new TestFixture();
		Course course = course(1L, 10L, CourseStatus.DRAFT);
		course.setCoverFileId(2L);
		FileAsset fileAsset = fileAsset(2L, 10L, FileAssetType.COURSE_COVER);
		fileAsset.setRelatedType("COURSE");
		fileAsset.setRelatedId(1L);
		when(fixture.courseRepository.findById(1L)).thenReturn(Optional.of(course));
		when(fixture.fileAssetRepository.findById(2L)).thenReturn(Optional.of(fileAsset));
		authenticate(teacher(10L));

		fixture.service.unbindCourseCover(1L);

		assertThat(course.getCoverFileId()).isNull();
		assertThat(fileAsset.getRelatedType()).isNull();
		assertThat(fileAsset.getRelatedId()).isNull();
	}

	@Test
	void unbindLessonAudioClearsLessonAndRelatedFileMetadata() {
		TestFixture fixture = new TestFixture();
		Course course = course(1L, 10L, CourseStatus.DRAFT);
		Lesson lesson = lesson(3L, 1L, LessonStatus.DRAFT);
		lesson.setAudioFileId(4L);
		FileAsset fileAsset = fileAsset(4L, 10L, FileAssetType.LESSON_AUDIO);
		fileAsset.setRelatedType("LESSON");
		fileAsset.setRelatedId(3L);
		when(fixture.lessonRepository.findById(3L)).thenReturn(Optional.of(lesson));
		when(fixture.courseRepository.findById(1L)).thenReturn(Optional.of(course));
		when(fixture.fileAssetRepository.findById(4L)).thenReturn(Optional.of(fileAsset));
		authenticate(teacher(10L));

		fixture.service.unbindLessonAudio(3L);

		assertThat(lesson.getAudioFileId()).isNull();
		assertThat(fileAsset.getRelatedType()).isNull();
		assertThat(fileAsset.getRelatedId()).isNull();
	}

	@Test
	void deleteRejectsFileStillBoundToCourse() {
		TestFixture fixture = new TestFixture();
		FileAsset fileAsset = fileAsset(1L, 10L, FileAssetType.COURSE_COVER);
		when(fixture.fileAssetRepository.findById(1L)).thenReturn(Optional.of(fileAsset));
		when(fixture.courseRepository.existsByCoverFileId(1L)).thenReturn(true);
		authenticate(teacher(10L));

		assertThatThrownBy(() -> fixture.service.deleteFile(1L))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.FILE_IN_USE.name());
		verifyNoInteractions(fixture.storageService);
	}

	private MockMultipartFile multipart(String originalName, String contentType, byte[] content) {
		return new MockMultipartFile("file", originalName, contentType, content);
	}

	private void authenticate(AuthenticatedUser user) {
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities())
		);
	}

	private AuthenticatedUser admin(Long id) {
		return user(id, "ROLE_ADMIN");
	}

	private AuthenticatedUser teacher(Long id) {
		return user(id, "ROLE_TEACHER");
	}

	private AuthenticatedUser student(Long id) {
		return user(id, "ROLE_STUDENT");
	}

	private AuthenticatedUser user(Long id, String authority) {
		return new AuthenticatedUser(
			id,
			"user%s@example.com".formatted(id),
			"user%s".formatted(id),
			"password",
			UserStatus.ACTIVE,
			List.of(new SimpleGrantedAuthority(authority))
		);
	}

	private FileAsset fileAsset(Long id, Long ownerId, FileAssetType assetType) {
		FileAsset fileAsset = new FileAsset();
		fileAsset.setId(id);
		fileAsset.setOriginalName("file-%s".formatted(id));
		fileAsset.setStorageKey("database/key-%s".formatted(id));
		fileAsset.setUrl("http://localhost:8080/api/v1/files/%s/content".formatted(id));
		fileAsset.setContentType(assetType == FileAssetType.LESSON_AUDIO ? "audio/mpeg" : "image/png");
		fileAsset.setSizeBytes(10L);
		fileAsset.setAssetType(assetType);
		fileAsset.setStorageProvider(StorageProvider.DATABASE);
		fileAsset.setOwnerId(ownerId);
		fileAsset.setChecksum("checksum");
		return fileAsset;
	}

	private Course course(Long id, Long ownerId, CourseStatus status) {
		Course course = new Course();
		course.setId(id);
		course.setOwnerId(ownerId);
		course.setTitle("Course %s".formatted(id));
		course.setStatus(status);
		return course;
	}

	private Lesson lesson(Long id, Long courseId, LessonStatus status) {
		Lesson lesson = new Lesson();
		lesson.setId(id);
		lesson.setCourseId(courseId);
		lesson.setTitle("Lesson %s".formatted(id));
		lesson.setSortOrder(0);
		lesson.setStatus(status);
		return lesson;
	}

	private static final class TestFixture {

		private final FileAssetRepository fileAssetRepository = mock(FileAssetRepository.class);

		private final CourseRepository courseRepository = mock(CourseRepository.class);

		private final LessonRepository lessonRepository = mock(LessonRepository.class);

		private final StorageService storageService = mock(StorageService.class);

		private final StorageProperties storageProperties = new StorageProperties();

		private final FileAccessProperties fileAccessProperties = new FileAccessProperties();

		private final FileAccessTokenService fileAccessTokenService = new FileAccessTokenService(fileAccessProperties);

		private final FileAssetServiceImpl service;

		private TestFixture() {
			storageProperties.setProvider(StorageProvider.DATABASE);
			storageProperties.setMaxFileSizeMb(20);
			storageProperties.setPublicBaseUrl("http://localhost:8080");
			fileAccessProperties.setSecret("test-file-access-secret");
			fileAccessProperties.setDefaultExpirationMinutes(10);
			service = new FileAssetServiceImpl(
				fileAssetRepository,
				courseRepository,
				lessonRepository,
				storageService,
				storageProperties,
				fileAccessProperties,
				fileAccessTokenService
			);
		}
	}
}
