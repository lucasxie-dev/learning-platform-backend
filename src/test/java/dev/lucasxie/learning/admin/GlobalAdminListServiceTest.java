package dev.lucasxie.learning.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import dev.lucasxie.learning.auth.security.AuthenticatedUser;
import dev.lucasxie.learning.common.exception.BusinessException;
import dev.lucasxie.learning.common.exception.ErrorCode;
import dev.lucasxie.learning.course.Course;
import dev.lucasxie.learning.course.CourseRepository;
import dev.lucasxie.learning.course.CourseStatus;
import dev.lucasxie.learning.file.FileAsset;
import dev.lucasxie.learning.file.FileAssetRepository;
import dev.lucasxie.learning.file.FileAssetType;
import dev.lucasxie.learning.file.StorageProvider;
import dev.lucasxie.learning.file.service.FileAssetService;
import dev.lucasxie.learning.lesson.Lesson;
import dev.lucasxie.learning.lesson.LessonRepository;
import dev.lucasxie.learning.lesson.LessonStatus;
import dev.lucasxie.learning.lesson.service.LessonService;
import dev.lucasxie.learning.user.UserStatus;

@SpringBootTest
@Transactional
class GlobalAdminListServiceTest {

	@Autowired
	private LessonService lessonService;

	@Autowired
	private FileAssetService fileAssetService;

	@Autowired
	private CourseRepository courseRepository;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private FileAssetRepository fileAssetRepository;

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void adminListsAllLessons() {
		Course ownedCourse = saveCourse(10L, CourseStatus.PUBLISHED);
		Course otherCourse = saveCourse(20L, CourseStatus.PUBLISHED);
		Lesson ownedLesson = saveLesson(ownedCourse, "Owned lesson", LessonStatus.DRAFT);
		Lesson otherLesson = saveLesson(otherCourse, "Other lesson", LessonStatus.PUBLISHED);
		authenticate(admin(1L));

		var response = lessonService.listLessons(null, null, null, null, null, null, 0, 20);

		assertThat(response.getItems()).extracting("id")
			.containsExactlyInAnyOrder(ownedLesson.getId(), otherLesson.getId());
		assertThat(response.getTotal()).isEqualTo(2);
	}

	@Test
	void teacherListsOnlyLessonsUnderOwnedCourses() {
		Course ownedCourse = saveCourse(10L, CourseStatus.PUBLISHED);
		Course otherCourse = saveCourse(20L, CourseStatus.PUBLISHED);
		Lesson ownedLesson = saveLesson(ownedCourse, "Owned lesson", LessonStatus.DRAFT);
		saveLesson(otherCourse, "Other lesson", LessonStatus.PUBLISHED);
		authenticate(teacher(10L));

		var response = lessonService.listLessons(null, null, null, null, null, null, 0, 20);

		assertThat(response.getItems()).extracting("id").containsExactly(ownedLesson.getId());
		assertThat(response.getItems().getFirst().courseTitle()).isEqualTo(ownedCourse.getTitle());
		assertThat(response.getItems().getFirst().courseId()).isEqualTo(ownedCourse.getId());
	}

	@Test
	void studentCannotListGlobalLessons() {
		authenticate(student(30L));

		assertThatThrownBy(() -> lessonService.listLessons(null, null, null, null, null, null, 0, 20))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.LESSON_ACCESS_DENIED.name());
	}

	@Test
	void listLessonsFiltersByStatus() {
		Course course = saveCourse(10L, CourseStatus.PUBLISHED);
		Lesson draftLesson = saveLesson(course, "Draft lesson", LessonStatus.DRAFT);
		saveLesson(course, "Published lesson", LessonStatus.PUBLISHED);
		authenticate(admin(1L));

		var response = lessonService.listLessons(null, null, LessonStatus.DRAFT, null, null, null, 0, 20);

		assertThat(response.getItems()).extracting("id").containsExactly(draftLesson.getId());
		assertThat(response.getItems().getFirst().status()).isEqualTo(LessonStatus.DRAFT);
	}

	@Test
	void listLessonsFiltersByHasAudioAndCapsPageSize() {
		Course course = saveCourse(10L, CourseStatus.PUBLISHED);
		Lesson audioLesson = saveLesson(course, "Audio lesson", LessonStatus.PUBLISHED);
		audioLesson.setAudioFileId(100L);
		audioLesson.setDurationSeconds(120);
		Lesson textLesson = saveLesson(course, "Text lesson", LessonStatus.PUBLISHED);
		authenticate(admin(1L));

		var withAudio = lessonService.listLessons(null, null, null, true, null, null, 0, 200);
		var withoutAudio = lessonService.listLessons(null, null, null, false, null, null, 0, 20);

		assertThat(withAudio.getItems()).extracting("id").containsExactly(audioLesson.getId());
		assertThat(withAudio.getItems().getFirst().hasAudio()).isTrue();
		assertThat(withAudio.getItems().getFirst().durationSeconds()).isEqualTo(120);
		assertThat(withAudio.getSize()).isEqualTo(100);
		assertThat(withoutAudio.getItems()).extracting("id").containsExactly(textLesson.getId());
		assertThat(withoutAudio.getItems().getFirst().hasAudio()).isFalse();
	}

	@Test
	void adminListsAllFiles() {
		FileAsset ownedFile = saveFile(10L, FileAssetType.COURSE_COVER, StorageProvider.DATABASE);
		FileAsset otherFile = saveFile(20L, FileAssetType.LESSON_AUDIO, StorageProvider.MINIO);
		authenticate(admin(1L));

		var response = fileAssetService.listFiles(null, null, null, null, null, null, 0, 20);

		assertThat(response.getItems()).extracting("id")
			.containsExactlyInAnyOrder(ownedFile.getId(), otherFile.getId());
		assertThat(response.getItems()).allSatisfy(item -> {
			assertThat(item.contentType()).isNotBlank();
			assertThat(item.storageProvider()).isNotNull();
			assertThat(item.createdAt()).isNotNull();
		});
	}

	@Test
	void teacherListsOnlyAccessibleFiles() {
		Course ownedCourse = saveCourse(10L, CourseStatus.PUBLISHED);
		Course otherCourse = saveCourse(20L, CourseStatus.PUBLISHED);
		Lesson ownedLesson = saveLesson(ownedCourse, "Owned lesson", LessonStatus.PUBLISHED);
		FileAsset uploadedByTeacher = saveFile(10L, FileAssetType.ATTACHMENT, StorageProvider.DATABASE);
		FileAsset ownedCourseFile = saveFile(99L, FileAssetType.COURSE_COVER, StorageProvider.DATABASE);
		ownedCourseFile.setRelatedType("COURSE");
		ownedCourseFile.setRelatedId(ownedCourse.getId());
		FileAsset ownedLessonFile = saveFile(99L, FileAssetType.LESSON_AUDIO, StorageProvider.DATABASE);
		ownedLessonFile.setRelatedType("LESSON");
		ownedLessonFile.setRelatedId(ownedLesson.getId());
		saveFile(99L, FileAssetType.SUBTITLE, StorageProvider.DATABASE);
		FileAsset otherCourseFile = saveFile(99L, FileAssetType.COURSE_COVER, StorageProvider.DATABASE);
		otherCourseFile.setRelatedType("COURSE");
		otherCourseFile.setRelatedId(otherCourse.getId());
		authenticate(teacher(10L));

		var response = fileAssetService.listFiles(null, null, null, null, null, null, 0, 20);

		assertThat(response.getItems()).extracting("id")
			.containsExactlyInAnyOrder(uploadedByTeacher.getId(), ownedCourseFile.getId(), ownedLessonFile.getId());
	}

	@Test
	void studentCannotListGlobalFiles() {
		authenticate(student(30L));

		assertThatThrownBy(() -> fileAssetService.listFiles(null, null, null, null, null, null, 0, 20))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.FILE_ACCESS_DENIED.name());
	}

	@Test
	void listFilesFiltersByAssetType() {
		FileAsset audioFile = saveFile(10L, FileAssetType.LESSON_AUDIO, StorageProvider.DATABASE);
		saveFile(10L, FileAssetType.COURSE_COVER, StorageProvider.DATABASE);
		authenticate(admin(1L));

		var response = fileAssetService.listFiles(null, FileAssetType.LESSON_AUDIO, null, null, null, null, 0, 20);

		assertThat(response.getItems()).extracting("id").containsExactly(audioFile.getId());
		assertThat(response.getItems().getFirst().assetType()).isEqualTo(FileAssetType.LESSON_AUDIO);
	}

	@Test
	void listFilesFiltersByUnboundAndCapsPageSize() {
		FileAsset unboundFile = saveFile(10L, FileAssetType.ATTACHMENT, StorageProvider.DATABASE);
		FileAsset boundFile = saveFile(10L, FileAssetType.COURSE_COVER, StorageProvider.DATABASE);
		boundFile.setRelatedType("COURSE");
		boundFile.setRelatedId(100L);
		authenticate(admin(1L));

		var response = fileAssetService.listFiles(null, null, null, null, null, false, 0, 200);

		assertThat(response.getItems()).extracting("id").containsExactly(unboundFile.getId());
		assertThat(response.getItems().getFirst().bound()).isFalse();
		assertThat(response.getItems().getFirst().checksum()).isEqualTo("checksum-%s".formatted(unboundFile.getId()));
		assertThat(response.getSize()).isEqualTo(100);
	}

	private Course saveCourse(Long ownerId, CourseStatus status) {
		Course course = new Course();
		course.setOwnerId(ownerId);
		course.setTitle("Course owned by %s".formatted(ownerId));
		course.setStatus(status);
		return courseRepository.saveAndFlush(course);
	}

	private Lesson saveLesson(Course course, String title, LessonStatus status) {
		Lesson lesson = new Lesson();
		lesson.setCourseId(course.getId());
		lesson.setTitle(title);
		lesson.setDescription("Description for %s".formatted(title));
		lesson.setSortOrder(0);
		lesson.setStatus(status);
		return lessonRepository.saveAndFlush(lesson);
	}

	private FileAsset saveFile(Long ownerId, FileAssetType assetType, StorageProvider storageProvider) {
		FileAsset fileAsset = new FileAsset();
		fileAsset.setOwnerId(ownerId);
		fileAsset.setOriginalName("%s-%s.dat".formatted(assetType.name().toLowerCase(), ownerId));
		fileAsset.setStorageKey("database/key-%s-%s".formatted(assetType.name().toLowerCase(), ownerId));
		fileAsset.setUrl("/api/v1/files/content");
		fileAsset.setContentType(assetType == FileAssetType.LESSON_AUDIO ? "audio/mpeg" : "application/octet-stream");
		fileAsset.setSizeBytes(128L);
		fileAsset.setAssetType(assetType);
		fileAsset.setStorageProvider(storageProvider);
		fileAsset = fileAssetRepository.saveAndFlush(fileAsset);
		fileAsset.setChecksum("checksum-%s".formatted(fileAsset.getId()));
		return fileAsset;
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
}
