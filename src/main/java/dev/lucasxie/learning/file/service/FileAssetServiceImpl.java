package dev.lucasxie.learning.file.service;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import dev.lucasxie.learning.auth.security.AuthenticatedUser;
import dev.lucasxie.learning.auth.security.SecurityUtils;
import dev.lucasxie.learning.common.api.PageResponse;
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
import dev.lucasxie.learning.file.dto.FileAccessUrlResponse;
import dev.lucasxie.learning.file.dto.FileAssetListItemResponse;
import dev.lucasxie.learning.file.dto.FileAssetResponse;
import dev.lucasxie.learning.file.dto.FileUploadResponse;
import dev.lucasxie.learning.lesson.Lesson;
import dev.lucasxie.learning.lesson.LessonRepository;
import dev.lucasxie.learning.lesson.LessonStatus;
import dev.lucasxie.learning.storage.StorageDownloadResult;
import dev.lucasxie.learning.storage.StorageProperties;
import dev.lucasxie.learning.storage.StorageService;
import dev.lucasxie.learning.storage.StorageUploadCommand;
import dev.lucasxie.learning.storage.StorageUploadResult;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@Service
public class FileAssetServiceImpl implements FileAssetService {

	private static final int MAX_PAGE_SIZE = 100;

	private static final String RELATED_TYPE_COURSE = "COURSE";

	private static final String RELATED_TYPE_LESSON = "LESSON";

	private static final String PENDING_STORAGE_KEY_PREFIX = "pending/";

	private static final Map<FileAssetType, Set<String>> ALLOWED_CONTENT_TYPES = Map.of(
		FileAssetType.AVATAR, Set.of("image/jpeg", "image/png", "image/webp"),
		FileAssetType.COURSE_COVER, Set.of("image/jpeg", "image/png", "image/webp"),
		FileAssetType.LESSON_AUDIO, Set.of("audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav", "audio/mp4"),
		FileAssetType.LESSON_VIDEO, Set.of("video/mp4", "video/quicktime", "video/webm"),
		FileAssetType.SUBTITLE, Set.of("text/vtt", "application/x-subrip", "text/plain"),
		FileAssetType.ATTACHMENT, Set.of(
			"application/pdf",
			"text/plain",
			"image/jpeg",
			"image/png",
			"image/webp",
			"application/zip"
		)
	);

	private final FileAssetRepository fileAssetRepository;

	private final CourseRepository courseRepository;

	private final LessonRepository lessonRepository;

	private final StorageService storageService;

	private final StorageProperties storageProperties;

	private final FileAccessProperties fileAccessProperties;

	private final FileAccessTokenService fileAccessTokenService;

	public FileAssetServiceImpl(
		FileAssetRepository fileAssetRepository,
		CourseRepository courseRepository,
		LessonRepository lessonRepository,
		StorageService storageService,
		StorageProperties storageProperties,
		FileAccessProperties fileAccessProperties,
		FileAccessTokenService fileAccessTokenService
	) {
		this.fileAssetRepository = fileAssetRepository;
		this.courseRepository = courseRepository;
		this.lessonRepository = lessonRepository;
		this.storageService = storageService;
		this.storageProperties = storageProperties;
		this.fileAccessProperties = fileAccessProperties;
		this.fileAccessTokenService = fileAccessTokenService;
	}

	@Override
	@Transactional
	public FileUploadResponse upload(MultipartFile file, FileAssetType assetType) {
		AuthenticatedUser currentUser = requireCurrentUser();
		if (!hasRole(currentUser, "ADMIN") && !hasRole(currentUser, "TEACHER") && !hasAuthority(currentUser, "file:upload")) {
			throw new BusinessException(ErrorCode.FILE_ACCESS_DENIED, "You do not have permission to upload files");
		}

		validateUpload(file, assetType);

		FileAsset fileAsset = new FileAsset();
		fileAsset.setOriginalName(resolveOriginalName(file));
		fileAsset.setStorageKey(PENDING_STORAGE_KEY_PREFIX + java.util.UUID.randomUUID());
		fileAsset.setContentType(file.getContentType());
		fileAsset.setSizeBytes(file.getSize());
		fileAsset.setAssetType(assetType);
		fileAsset.setStorageProvider(storageProperties.getProvider() == null ? StorageProvider.DATABASE : storageProperties.getProvider());
		fileAsset.setOwnerId(currentUser.getId());
		fileAsset = fileAssetRepository.saveAndFlush(fileAsset);

		StorageUploadResult uploadResult = uploadToStorage(file, assetType, currentUser, fileAsset.getId());
		fileAsset.setStorageKey(uploadResult.storageKey());
		fileAsset.setUrl(buildContentUrl(fileAsset.getId()));
		fileAsset.setContentType(uploadResult.contentType());
		fileAsset.setSizeBytes(uploadResult.sizeBytes());
		fileAsset.setChecksum(uploadResult.checksum());
		fileAsset.setStorageProvider(uploadResult.storageProvider());

		return toUploadResponse(fileAsset);
	}

	@Override
	@Transactional(readOnly = true)
	public FileAssetResponse getFile(Long fileId) {
		FileAsset fileAsset = getExistingFile(fileId);
		requireViewAccess(requireCurrentUser(), fileAsset);

		return toResponse(fileAsset);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<FileAssetListItemResponse> listFiles(
		String keyword,
		FileAssetType assetType,
		StorageProvider storageProvider,
		String relatedType,
		Long relatedId,
		Boolean bound,
		int page,
		int size
	) {
		AuthenticatedUser currentUser = requireCurrentUser();
		requireAdminOrTeacher(currentUser, "Only admins and teachers can list files globally");

		Pageable pageable = PageRequest.of(
			Math.max(page, 0),
			Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
			Sort.by(Sort.Direction.DESC, "createdAt")
		);
		Page<FileAsset> files = fileAssetRepository.findAll(
			buildFileAssetSpecification(currentUser, keyword, assetType, storageProvider, relatedType, relatedId, bound),
			pageable
		);
		List<FileAssetListItemResponse> items = files.getContent()
			.stream()
			.map(this::toListItemResponse)
			.toList();

		return new PageResponse<>(
			items,
			files.getTotalElements(),
			files.getNumber(),
			files.getSize(),
			files.getTotalPages()
		);
	}

	@Override
	@Transactional(readOnly = true)
	public FileDownload downloadFileContent(Long fileId) {
		FileAsset fileAsset = getExistingFile(fileId);
		requireViewAccess(requireCurrentUser(), fileAsset);

		return download(fileAsset);
	}

	@Override
	@Transactional(readOnly = true)
	public FileDownload downloadPublicFileContent(Long fileId, String token) {
		fileAccessTokenService.validateToken(fileId, token);
		return download(getExistingFile(fileId));
	}

	@Override
	@Transactional(readOnly = true)
	public FileAccessUrlResponse createAccessUrl(Long fileId) {
		FileAsset fileAsset = getExistingFile(fileId);
		requireViewAccess(requireCurrentUser(), fileAsset);

		Instant expiresAt = Instant.now()
			.plus(fileAccessProperties.getDefaultExpirationMinutes(), ChronoUnit.MINUTES)
			.truncatedTo(ChronoUnit.SECONDS);
		String token = fileAccessTokenService.generateToken(fileId, expiresAt);

		return new FileAccessUrlResponse(buildPublicContentUrl(fileId, token), expiresAt);
	}

	@Override
	@Transactional
	public void deleteFile(Long fileId) {
		FileAsset fileAsset = getExistingFile(fileId);
		AuthenticatedUser currentUser = requireCurrentUser();
		if (!hasRole(currentUser, "ADMIN") && !fileAsset.getOwnerId().equals(currentUser.getId())) {
			throw new BusinessException(ErrorCode.FILE_ACCESS_DENIED, "Only admins and file owners can delete files");
		}

		requireNotInUse(fileAsset);
		storageService.delete(fileAsset.getStorageKey());
		fileAssetRepository.delete(fileAsset);
	}

	@Override
	@Transactional
	public FileAssetResponse bindCourseCover(Long courseId, Long fileId) {
		Course course = getExistingCourse(courseId);
		AuthenticatedUser currentUser = requireCurrentUser();
		requireCourseManageAccess(currentUser, course, "Only admins and course owners can bind course covers");

		FileAsset fileAsset = getExistingFile(fileId);
		requireBindingAccess(currentUser, fileAsset);
		requireAssetType(fileAsset, FileAssetType.COURSE_COVER);

		course.setCoverFileId(fileAsset.getId());
		fileAsset.setRelatedType(RELATED_TYPE_COURSE);
		fileAsset.setRelatedId(course.getId());

		return toResponse(fileAsset);
	}

	@Override
	@Transactional
	public FileAssetResponse bindLessonAudio(Long lessonId, Long fileId) {
		return bindLessonMedia(lessonId, fileId, FileAssetType.LESSON_AUDIO);
	}

	@Override
	@Transactional
	public FileAssetResponse bindLessonVideo(Long lessonId, Long fileId) {
		return bindLessonMedia(lessonId, fileId, FileAssetType.LESSON_VIDEO);
	}

	@Override
	@Transactional
	public FileAssetResponse bindLessonSubtitle(Long lessonId, Long fileId) {
		return bindLessonMedia(lessonId, fileId, FileAssetType.SUBTITLE);
	}

	@Override
	@Transactional
	public void unbindCourseCover(Long courseId) {
		Course course = getExistingCourse(courseId);
		AuthenticatedUser currentUser = requireCurrentUser();
		requireCourseManageAccess(currentUser, course, "Only admins and course owners can unbind course covers");

		clearRelatedFile(RELATED_TYPE_COURSE, course.getId(), course.getCoverFileId());
		course.setCoverFileId(null);
	}

	@Override
	@Transactional
	public void unbindLessonAudio(Long lessonId) {
		unbindLessonMedia(lessonId, LessonMediaSlot.AUDIO);
	}

	@Override
	@Transactional
	public void unbindLessonVideo(Long lessonId) {
		unbindLessonMedia(lessonId, LessonMediaSlot.VIDEO);
	}

	@Override
	@Transactional
	public void unbindLessonSubtitle(Long lessonId) {
		unbindLessonMedia(lessonId, LessonMediaSlot.SUBTITLE);
	}

	private FileAssetResponse bindLessonMedia(Long lessonId, Long fileId, FileAssetType requiredAssetType) {
		Lesson lesson = getExistingLesson(lessonId);
		Course course = getExistingCourse(lesson.getCourseId());
		AuthenticatedUser currentUser = requireCurrentUser();
		requireCourseManageAccess(currentUser, course, "Only admins and course owners can bind lesson media");

		FileAsset fileAsset = getExistingFile(fileId);
		requireBindingAccess(currentUser, fileAsset);
		requireAssetType(fileAsset, requiredAssetType);

		if (requiredAssetType == FileAssetType.LESSON_AUDIO) {
			lesson.setAudioFileId(fileAsset.getId());
		}
		else if (requiredAssetType == FileAssetType.LESSON_VIDEO) {
			lesson.setVideoFileId(fileAsset.getId());
		}
		else if (requiredAssetType == FileAssetType.SUBTITLE) {
			lesson.setSubtitleFileId(fileAsset.getId());
		}

		fileAsset.setRelatedType(RELATED_TYPE_LESSON);
		fileAsset.setRelatedId(lesson.getId());

		return toResponse(fileAsset);
	}

	private void unbindLessonMedia(Long lessonId, LessonMediaSlot mediaSlot) {
		Lesson lesson = getExistingLesson(lessonId);
		Course course = getExistingCourse(lesson.getCourseId());
		AuthenticatedUser currentUser = requireCurrentUser();
		requireCourseManageAccess(currentUser, course, "Only admins and course owners can unbind lesson media");

		Long fileId = switch (mediaSlot) {
			case AUDIO -> lesson.getAudioFileId();
			case VIDEO -> lesson.getVideoFileId();
			case SUBTITLE -> lesson.getSubtitleFileId();
		};

		clearRelatedFile(RELATED_TYPE_LESSON, lesson.getId(), fileId);

		switch (mediaSlot) {
			case AUDIO -> lesson.setAudioFileId(null);
			case VIDEO -> lesson.setVideoFileId(null);
			case SUBTITLE -> lesson.setSubtitleFileId(null);
		}
	}

	private void clearRelatedFile(String relatedType, Long relatedId, Long fileId) {
		if (fileId == null) {
			return;
		}

		fileAssetRepository.findById(fileId)
			.filter(fileAsset -> relatedType.equals(fileAsset.getRelatedType()))
			.filter(fileAsset -> relatedId.equals(fileAsset.getRelatedId()))
			.ifPresent(fileAsset -> {
				fileAsset.setRelatedType(null);
				fileAsset.setRelatedId(null);
			});
	}

	private StorageUploadResult uploadToStorage(
		MultipartFile file,
		FileAssetType assetType,
		AuthenticatedUser currentUser,
		Long fileAssetId
	) {
		try {
			return storageService.upload(new StorageUploadCommand(
				resolveOriginalName(file),
				file.getContentType(),
				file.getSize(),
				file.getInputStream(),
				assetType,
				currentUser.getId(),
				fileAssetId
			));
		}
		catch (IOException exception) {
			throw new BusinessException(ErrorCode.STORAGE_ERROR, "Failed to read uploaded file");
		}
	}

	private void validateUpload(MultipartFile file, FileAssetType assetType) {
		if (assetType == null) {
			throw new BusinessException(ErrorCode.FILE_BINDING_INVALID, "File asset type is required");
		}

		if (file == null || file.isEmpty()) {
			throw new BusinessException(ErrorCode.FILE_EMPTY, "Uploaded file must not be empty");
		}

		long maxFileSizeBytes = storageProperties.getMaxFileSizeMb() * 1024L * 1024L;
		if (file.getSize() > maxFileSizeBytes) {
			throw new BusinessException(ErrorCode.FILE_TOO_LARGE, "Uploaded file exceeds the maximum allowed size");
		}

		String contentType = file.getContentType();
		if (!StringUtils.hasText(contentType) || !ALLOWED_CONTENT_TYPES.getOrDefault(assetType, Set.of()).contains(contentType)) {
			throw new BusinessException(ErrorCode.FILE_UNSUPPORTED_TYPE, "Unsupported content type for file asset type");
		}
	}

	private void requireViewAccess(AuthenticatedUser currentUser, FileAsset fileAsset) {
		if (hasRole(currentUser, "ADMIN") || fileAsset.getOwnerId().equals(currentUser.getId())) {
			return;
		}

		if (RELATED_TYPE_COURSE.equals(fileAsset.getRelatedType()) && fileAsset.getRelatedId() != null) {
			Course course = getExistingCourse(fileAsset.getRelatedId());
			if (course.getStatus() == CourseStatus.PUBLISHED) {
				return;
			}
		}

		if (RELATED_TYPE_LESSON.equals(fileAsset.getRelatedType()) && fileAsset.getRelatedId() != null) {
			Lesson lesson = getExistingLesson(fileAsset.getRelatedId());
			Course course = getExistingCourse(lesson.getCourseId());
			if (course.getStatus() == CourseStatus.PUBLISHED && lesson.getStatus() == LessonStatus.PUBLISHED) {
				return;
			}
		}

		if (isReferencedByPublishedLessonMarkdown(fileAsset)) {
			return;
		}

		throw new BusinessException(ErrorCode.FILE_ACCESS_DENIED, "You do not have access to this file");
	}

	private void requireBindingAccess(AuthenticatedUser currentUser, FileAsset fileAsset) {
		if (hasRole(currentUser, "ADMIN")) {
			return;
		}

		if (fileAsset.getOwnerId().equals(currentUser.getId())) {
			return;
		}

		throw new BusinessException(ErrorCode.FILE_ACCESS_DENIED, "Only admins and file owners can bind files");
	}

	private void requireCourseManageAccess(AuthenticatedUser currentUser, Course course, String message) {
		if (hasRole(currentUser, "ADMIN")) {
			return;
		}

		if (hasRole(currentUser, "TEACHER") && course.getOwnerId().equals(currentUser.getId())) {
			return;
		}

		throw new BusinessException(ErrorCode.FILE_ACCESS_DENIED, message);
	}

	private void requireAdminOrTeacher(AuthenticatedUser currentUser, String message) {
		if (hasRole(currentUser, "ADMIN") || hasRole(currentUser, "TEACHER")) {
			return;
		}

		throw new BusinessException(ErrorCode.FILE_ACCESS_DENIED, message);
	}

	private void requireAssetType(FileAsset fileAsset, FileAssetType requiredAssetType) {
		if (fileAsset.getAssetType() != requiredAssetType) {
			throw new BusinessException(ErrorCode.FILE_BINDING_INVALID, "File asset type is not valid for this binding");
		}
	}

	private void requireNotInUse(FileAsset fileAsset) {
		if (courseRepository.existsByCoverFileId(fileAsset.getId())
			|| lessonRepository.existsByAudioFileIdOrVideoFileIdOrSubtitleFileId(fileAsset.getId(), fileAsset.getId(), fileAsset.getId())
			|| isReferencedByAnyLessonMarkdown(fileAsset)) {
			throw new BusinessException(ErrorCode.FILE_IN_USE, "File is still referenced by course cover, lesson media, or lesson Markdown; remove the reference before deleting");
		}
	}

	private boolean isReferencedByPublishedLessonMarkdown(FileAsset fileAsset) {
		String fileReference = "file:" + fileAsset.getId();
		return lessonRepository.findByContentMarkdownContaining(fileReference)
			.stream()
			.filter(lesson -> containsExactFileReference(lesson.getContentMarkdown(), fileAsset.getId()))
			.anyMatch(lesson -> {
				Course course = getExistingCourse(lesson.getCourseId());
				return course.getStatus() == CourseStatus.PUBLISHED && lesson.getStatus() == LessonStatus.PUBLISHED;
			});
	}

	private boolean isReferencedByAnyLessonMarkdown(FileAsset fileAsset) {
		String fileReference = "file:" + fileAsset.getId();
		return lessonRepository.findByContentMarkdownContaining(fileReference)
			.stream()
			.anyMatch(lesson -> containsExactFileReference(lesson.getContentMarkdown(), fileAsset.getId()));
	}

	private boolean containsExactFileReference(String contentMarkdown, Long fileId) {
		if (!StringUtils.hasText(contentMarkdown)) {
			return false;
		}

		String fileReference = "file:" + fileId;
		int index = contentMarkdown.indexOf(fileReference);
		while (index >= 0) {
			int endIndex = index + fileReference.length();
			if (endIndex >= contentMarkdown.length() || !Character.isDigit(contentMarkdown.charAt(endIndex))) {
				return true;
			}
			index = contentMarkdown.indexOf(fileReference, endIndex);
		}

		return false;
	}

	private FileAsset getExistingFile(Long fileId) {
		return fileAssetRepository.findById(fileId)
			.orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND, "File was not found"));
	}

	private Course getExistingCourse(Long courseId) {
		return courseRepository.findById(courseId)
			.orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND, "Course was not found"));
	}

	private Lesson getExistingLesson(Long lessonId) {
		return lessonRepository.findById(lessonId)
			.orElseThrow(() -> new BusinessException(ErrorCode.LESSON_NOT_FOUND, "Lesson was not found"));
	}

	private AuthenticatedUser requireCurrentUser() {
		return SecurityUtils.getCurrentUser()
			.orElseThrow(() -> new BusinessException(ErrorCode.COMMON_UNAUTHORIZED, "Authentication is required"));
	}

	private boolean hasRole(AuthenticatedUser user, String role) {
		return hasAuthority(user, "ROLE_" + role);
	}

	private boolean hasAuthority(AuthenticatedUser user, String authority) {
		Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
		return authorities.stream().anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
	}

	private Specification<FileAsset> buildFileAssetSpecification(
		AuthenticatedUser currentUser,
		String keyword,
		FileAssetType assetType,
		StorageProvider storageProvider,
		String relatedType,
		Long relatedId,
		Boolean bound
	) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (hasRole(currentUser, "TEACHER") && !hasRole(currentUser, "ADMIN")) {
				Subquery<Long> courseBindingCourseIds = query.subquery(Long.class);
				Root<Course> courseRoot = courseBindingCourseIds.from(Course.class);
				courseBindingCourseIds.select(courseRoot.get("id"))
					.where(criteriaBuilder.equal(courseRoot.get("ownerId"), currentUser.getId()));

				Subquery<Long> ownedLessonIds = query.subquery(Long.class);
				Root<Lesson> lessonRoot = ownedLessonIds.from(Lesson.class);
				Root<Course> lessonCourseRoot = ownedLessonIds.from(Course.class);
				ownedLessonIds.select(lessonRoot.get("id"))
					.where(
						criteriaBuilder.equal(lessonRoot.get("courseId"), lessonCourseRoot.get("id")),
						criteriaBuilder.equal(lessonCourseRoot.get("ownerId"), currentUser.getId())
					);

				predicates.add(criteriaBuilder.or(
					criteriaBuilder.equal(root.get("ownerId"), currentUser.getId()),
					criteriaBuilder.and(
						criteriaBuilder.equal(root.get("relatedType"), RELATED_TYPE_COURSE),
						root.get("relatedId").in(courseBindingCourseIds)
					),
					criteriaBuilder.and(
						criteriaBuilder.equal(root.get("relatedType"), RELATED_TYPE_LESSON),
						root.get("relatedId").in(ownedLessonIds)
					)
				));
			}

			if (assetType != null) {
				predicates.add(criteriaBuilder.equal(root.get("assetType"), assetType));
			}

			if (storageProvider != null) {
				predicates.add(criteriaBuilder.equal(root.get("storageProvider"), storageProvider));
			}

			if (StringUtils.hasText(relatedType)) {
				predicates.add(criteriaBuilder.equal(root.get("relatedType"), relatedType.trim()));
			}

			if (relatedId != null) {
				predicates.add(criteriaBuilder.equal(root.get("relatedId"), relatedId));
			}

			if (bound != null) {
				predicates.add(Boolean.TRUE.equals(bound)
					? criteriaBuilder.and(
						criteriaBuilder.isNotNull(root.get("relatedType")),
						criteriaBuilder.isNotNull(root.get("relatedId"))
					)
					: criteriaBuilder.or(
						criteriaBuilder.isNull(root.get("relatedType")),
						criteriaBuilder.isNull(root.get("relatedId"))
					));
			}

			if (StringUtils.hasText(keyword)) {
				String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
				predicates.add(criteriaBuilder.or(
					criteriaBuilder.like(criteriaBuilder.lower(root.get("originalName")), pattern),
					criteriaBuilder.like(criteriaBuilder.lower(root.get("contentType")), pattern)
				));
			}

			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private String resolveOriginalName(MultipartFile file) {
		if (StringUtils.hasText(file.getOriginalFilename())) {
			return file.getOriginalFilename().trim();
		}

		return "upload";
	}

	private String buildContentUrl(Long fileId) {
		String publicBaseUrl = storageProperties.getPublicBaseUrl();
		String path = "/api/v1/files/" + fileId + "/content";
		if (!StringUtils.hasText(publicBaseUrl)) {
			return path;
		}

		String normalizedBaseUrl = publicBaseUrl.endsWith("/")
			? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
			: publicBaseUrl;
		return normalizedBaseUrl + path;
	}

	private String buildPublicContentUrl(Long fileId, String token) {
		String publicBaseUrl = storageProperties.getPublicBaseUrl();
		String path = "/api/v1/files/" + fileId + "/public-content?token=" + token;
		if (!StringUtils.hasText(publicBaseUrl)) {
			return path;
		}

		String normalizedBaseUrl = publicBaseUrl.endsWith("/")
			? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
			: publicBaseUrl;
		return normalizedBaseUrl + path;
	}

	private FileDownload download(FileAsset fileAsset) {
		StorageDownloadResult downloadResult = storageService.download(fileAsset.getStorageKey());
		return new FileDownload(
			downloadResult.content(),
			downloadResult.contentType(),
			downloadResult.sizeBytes(),
			downloadResult.originalName()
		);
	}

	private FileUploadResponse toUploadResponse(FileAsset fileAsset) {
		return new FileUploadResponse(
			fileAsset.getId(),
			fileAsset.getOriginalName(),
			fileAsset.getUrl(),
			fileAsset.getContentType(),
			fileAsset.getSizeBytes(),
			fileAsset.getAssetType(),
			fileAsset.getStorageProvider(),
			fileAsset.getChecksum(),
			fileAsset.getCreatedAt()
		);
	}

	private FileAssetListItemResponse toListItemResponse(FileAsset fileAsset) {
		return new FileAssetListItemResponse(
			fileAsset.getId(),
			fileAsset.getOriginalName(),
			fileAsset.getUrl(),
			fileAsset.getContentType(),
			fileAsset.getSizeBytes(),
			fileAsset.getAssetType(),
			fileAsset.getStorageProvider(),
			fileAsset.getOwnerId(),
			fileAsset.getRelatedType(),
			fileAsset.getRelatedId(),
			fileAsset.getRelatedType() != null && fileAsset.getRelatedId() != null,
			fileAsset.getChecksum(),
			fileAsset.getCreatedAt(),
			fileAsset.getUpdatedAt()
		);
	}

	private FileAssetResponse toResponse(FileAsset fileAsset) {
		return new FileAssetResponse(
			fileAsset.getId(),
			fileAsset.getOriginalName(),
			fileAsset.getUrl(),
			fileAsset.getContentType(),
			fileAsset.getSizeBytes(),
			fileAsset.getAssetType(),
			fileAsset.getStorageProvider(),
			fileAsset.getOwnerId(),
			fileAsset.getRelatedType(),
			fileAsset.getRelatedId(),
			fileAsset.getChecksum(),
			fileAsset.getCreatedAt(),
			fileAsset.getUpdatedAt()
		);
	}

	private enum LessonMediaSlot {

		AUDIO,

		VIDEO,

		SUBTITLE
	}
}
