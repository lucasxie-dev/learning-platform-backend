package dev.lucasxie.learning.dashboard.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.lucasxie.learning.auth.security.AuthenticatedUser;
import dev.lucasxie.learning.auth.security.SecurityUtils;
import dev.lucasxie.learning.common.exception.BusinessException;
import dev.lucasxie.learning.common.exception.ErrorCode;
import dev.lucasxie.learning.course.Course;
import dev.lucasxie.learning.course.CourseEnrollment;
import dev.lucasxie.learning.course.CourseEnrollmentRepository;
import dev.lucasxie.learning.course.CourseRepository;
import dev.lucasxie.learning.course.CourseStatus;
import dev.lucasxie.learning.dashboard.dto.DashboardActivityResponse;
import dev.lucasxie.learning.dashboard.dto.DashboardCourseResponse;
import dev.lucasxie.learning.dashboard.dto.DashboardLearningResponse;
import dev.lucasxie.learning.dashboard.dto.DashboardLessonPreviewResponse;
import dev.lucasxie.learning.dashboard.dto.DashboardMediaResponse;
import dev.lucasxie.learning.dashboard.dto.DashboardResponse;
import dev.lucasxie.learning.dashboard.dto.DashboardSummaryResponse;
import dev.lucasxie.learning.file.FileAsset;
import dev.lucasxie.learning.file.FileAssetRepository;
import dev.lucasxie.learning.file.FileAssetType;
import dev.lucasxie.learning.lesson.Lesson;
import dev.lucasxie.learning.lesson.LessonProgress;
import dev.lucasxie.learning.lesson.LessonProgressRepository;
import dev.lucasxie.learning.lesson.LessonRepository;
import dev.lucasxie.learning.lesson.LessonStatus;

@Service
public class DashboardServiceImpl implements DashboardService {

	private static final int RECENT_LIMIT = 5;

	private static final Set<FileAssetType> MEDIA_TYPES = Set.of(
		FileAssetType.COURSE_COVER,
		FileAssetType.LESSON_AUDIO,
		FileAssetType.LESSON_VIDEO,
		FileAssetType.SUBTITLE,
		FileAssetType.ATTACHMENT
	);

	private final CourseRepository courseRepository;

	private final LessonRepository lessonRepository;

	private final CourseEnrollmentRepository courseEnrollmentRepository;

	private final LessonProgressRepository lessonProgressRepository;

	private final FileAssetRepository fileAssetRepository;

	public DashboardServiceImpl(
		CourseRepository courseRepository,
		LessonRepository lessonRepository,
		CourseEnrollmentRepository courseEnrollmentRepository,
		LessonProgressRepository lessonProgressRepository,
		FileAssetRepository fileAssetRepository
	) {
		this.courseRepository = courseRepository;
		this.lessonRepository = lessonRepository;
		this.courseEnrollmentRepository = courseEnrollmentRepository;
		this.lessonProgressRepository = lessonProgressRepository;
		this.fileAssetRepository = fileAssetRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public DashboardResponse getDashboard() {
		AuthenticatedUser currentUser = SecurityUtils.getCurrentUser()
			.orElseThrow(() -> new BusinessException(ErrorCode.COMMON_UNAUTHORIZED, "Authentication is required"));

		boolean admin = hasRole(currentUser, "ADMIN");
		boolean teacher = hasRole(currentUser, "TEACHER");
		boolean student = hasRole(currentUser, "STUDENT");
		boolean canManageCourses = admin || teacher || hasCourseAuthority(currentUser);

		List<Long> managedCourseIds = getManagedCourseIds(currentUser, admin, canManageCourses);
		List<Long> learningCourseIds = getLearningCourseIds(currentUser, student);
		List<Long> dashboardCourseIds = canManageCourses ? managedCourseIds : learningCourseIds;

		DashboardSummaryResponse summary = buildSummary(currentUser, admin, canManageCourses, managedCourseIds, learningCourseIds);
		List<DashboardCourseResponse> recentCourses = buildRecentCourses(currentUser, admin, canManageCourses, managedCourseIds);
		List<DashboardLearningResponse> recentLearning = buildRecentLearning(currentUser, student);
		List<DashboardMediaResponse> recentMedia = buildRecentMedia(currentUser, admin, canManageCourses);
		DashboardLessonPreviewResponse lessonPreview = buildLessonPreview(dashboardCourseIds);
		List<DashboardActivityResponse> recentActivities = buildRecentActivities(currentUser, admin, canManageCourses, managedCourseIds, student);

		return new DashboardResponse(
			summary,
			recentCourses,
			recentLearning,
			recentMedia,
			lessonPreview,
			recentActivities
		);
	}

	private DashboardSummaryResponse buildSummary(
		AuthenticatedUser currentUser,
		boolean admin,
		boolean canManageCourses,
		List<Long> managedCourseIds,
		List<Long> learningCourseIds
	) {
		if (canManageCourses) {
			long totalCourses = admin ? courseRepository.count() : courseRepository.countByOwnerId(currentUser.getId());
			long publishedCourses = admin
				? courseRepository.countByStatus(CourseStatus.PUBLISHED)
				: courseRepository.countByOwnerIdAndStatus(currentUser.getId(), CourseStatus.PUBLISHED);
			long draftCourses = admin
				? courseRepository.countByStatus(CourseStatus.DRAFT)
				: courseRepository.countByOwnerIdAndStatus(currentUser.getId(), CourseStatus.DRAFT);
			long archivedCourses = admin
				? courseRepository.countByStatus(CourseStatus.ARCHIVED)
				: courseRepository.countByOwnerIdAndStatus(currentUser.getId(), CourseStatus.ARCHIVED);
			long totalLessons = countLessons(managedCourseIds);
			long totalEnrollments = countEnrollments(managedCourseIds);
			long totalMediaAssets = admin
				? fileAssetRepository.countByAssetTypeIn(MEDIA_TYPES)
				: fileAssetRepository.countByOwnerIdAndAssetTypeIn(currentUser.getId(), MEDIA_TYPES);
			double averageCompletionRate = calculateManagedCompletionRate(managedCourseIds, totalLessons, totalEnrollments);

			return new DashboardSummaryResponse(
				totalCourses,
				publishedCourses,
				draftCourses,
				archivedCourses,
				totalLessons,
				totalEnrollments,
				totalMediaAssets,
				averageCompletionRate
			);
		}

		long totalCourses = learningCourseIds.size();
		long totalLessons = learningCourseIds.isEmpty()
			? 0
			: lessonRepository.countByCourseIdInAndStatus(learningCourseIds, LessonStatus.PUBLISHED);
		long completedLessons = learningCourseIds.isEmpty()
			? 0
			: lessonProgressRepository.countByUserIdAndCourseIdInAndCompletedTrue(currentUser.getId(), learningCourseIds);
		double averageCompletionRate = totalLessons == 0 ? 0 : (double) completedLessons / totalLessons;

		return new DashboardSummaryResponse(
			totalCourses,
			0,
			0,
			0,
			totalLessons,
			totalCourses,
			0,
			averageCompletionRate
		);
	}

	private List<DashboardCourseResponse> buildRecentCourses(
		AuthenticatedUser currentUser,
		boolean admin,
		boolean canManageCourses,
		List<Long> managedCourseIds
	) {
		List<Course> courses;
		if (canManageCourses) {
			if (managedCourseIds.isEmpty()) {
				return List.of();
			}
			List<Long> recentIds = admin
				? courseRepository.findRecentCourseIds(PageRequest.of(0, RECENT_LIMIT))
				: courseRepository.findRecentCourseIdsByOwnerId(currentUser.getId(), PageRequest.of(0, RECENT_LIMIT));
			courses = recentIds.isEmpty() ? List.of() : courseRepository.findByIdInOrderByCreatedAtDesc(recentIds);
		}
		else {
			courses = courseRepository.findRecentByStatus(CourseStatus.PUBLISHED, PageRequest.of(0, RECENT_LIMIT));
		}

		return courses.stream()
			.map(this::toCourseResponse)
			.toList();
	}

	private List<DashboardLearningResponse> buildRecentLearning(AuthenticatedUser currentUser, boolean student) {
		if (!student) {
			return List.of();
		}

		List<CourseEnrollment> enrollments = courseEnrollmentRepository.findRecentByUserId(
			currentUser.getId(),
			PageRequest.of(0, RECENT_LIMIT)
		);
		if (enrollments.isEmpty()) {
			return List.of();
		}

		Map<Long, Course> coursesById = courseRepository.findAllById(
				enrollments.stream().map(CourseEnrollment::getCourseId).toList()
			)
			.stream()
			.collect(Collectors.toMap(Course::getId, Function.identity()));

		return enrollments.stream()
			.map(enrollment -> toLearningResponse(enrollment, coursesById.get(enrollment.getCourseId())))
			.filter(Objects::nonNull)
			.toList();
	}

	private List<DashboardMediaResponse> buildRecentMedia(
		AuthenticatedUser currentUser,
		boolean admin,
		boolean canManageCourses
	) {
		if (!canManageCourses) {
			return List.of();
		}

		List<FileAsset> files = admin
			? fileAssetRepository.findRecentByAssetTypes(MEDIA_TYPES, PageRequest.of(0, RECENT_LIMIT))
			: fileAssetRepository.findRecentByOwnerIdAndAssetTypes(currentUser.getId(), MEDIA_TYPES, PageRequest.of(0, RECENT_LIMIT));

		return files.stream()
			.map(this::toMediaResponse)
			.toList();
	}

	private DashboardLessonPreviewResponse buildLessonPreview(List<Long> courseIds) {
		if (courseIds.isEmpty()) {
			return null;
		}

		Optional<Lesson> latestLesson = lessonRepository
			.findRecentLessonsByCourseIds(courseIds, PageRequest.of(0, 1))
			.stream()
			.findFirst();
		if (latestLesson.isEmpty()) {
			return null;
		}

		Lesson lesson = latestLesson.get();
		String courseTitle = courseRepository.findById(lesson.getCourseId())
			.map(Course::getTitle)
			.orElse("Course");

		return new DashboardLessonPreviewResponse(
			lesson.getId(),
			lesson.getCourseId(),
			lesson.getTitle(),
			courseTitle,
			lesson.getAudioFileId(),
			lesson.getVideoFileId(),
			lesson.getSubtitleFileId(),
			lesson.getDurationSeconds()
		);
	}

	private List<DashboardActivityResponse> buildRecentActivities(
		AuthenticatedUser currentUser,
		boolean admin,
		boolean canManageCourses,
		List<Long> managedCourseIds,
		boolean student
	) {
		List<DashboardActivityResponse> activities = new ArrayList<>();

		if (canManageCourses && !managedCourseIds.isEmpty()) {
			courseRepository.findByIdInOrderByCreatedAtDesc(managedCourseIds.stream().limit(RECENT_LIMIT).toList())
				.forEach(course -> activities.add(new DashboardActivityResponse(
					"COURSE",
					"Course updated",
					course.getTitle(),
					coalesce(course.getUpdatedAt(), course.getCreatedAt())
				)));
			courseEnrollmentRepository.findRecentByCourseIds(managedCourseIds, PageRequest.of(0, RECENT_LIMIT))
				.forEach(enrollment -> activities.add(new DashboardActivityResponse(
					"ENROLLMENT",
					"New enrollment",
					"User #" + enrollment.getUserId() + " enrolled in course #" + enrollment.getCourseId(),
					enrollment.getEnrolledAt()
				)));
			lessonProgressRepository.findRecentByCourseIds(managedCourseIds, PageRequest.of(0, RECENT_LIMIT))
				.stream()
				.filter(progress -> Boolean.TRUE.equals(progress.getCompleted()))
				.forEach(progress -> activities.add(new DashboardActivityResponse(
					"LESSON_COMPLETED",
					"Lesson completed",
					"User #" + progress.getUserId() + " completed lesson #" + progress.getLessonId(),
					coalesce(progress.getCompletedAt(), progress.getUpdatedAt())
				)));
			List<FileAsset> files = admin
				? fileAssetRepository.findRecentByAssetTypes(MEDIA_TYPES, PageRequest.of(0, RECENT_LIMIT))
				: fileAssetRepository.findRecentByOwnerIdAndAssetTypes(currentUser.getId(), MEDIA_TYPES, PageRequest.of(0, RECENT_LIMIT));
			files.forEach(file -> activities.add(new DashboardActivityResponse(
				"MEDIA",
				"Media uploaded",
				file.getOriginalName(),
				file.getCreatedAt()
			)));
		}

		if (student) {
			courseEnrollmentRepository.findRecentByUserId(currentUser.getId(), PageRequest.of(0, RECENT_LIMIT))
				.forEach(enrollment -> activities.add(new DashboardActivityResponse(
					"ENROLLMENT",
					"Course enrolled",
					"Course #" + enrollment.getCourseId(),
					enrollment.getEnrolledAt()
				)));
			lessonProgressRepository.findRecentByUserId(currentUser.getId(), PageRequest.of(0, RECENT_LIMIT))
				.forEach(progress -> activities.add(new DashboardActivityResponse(
					Boolean.TRUE.equals(progress.getCompleted()) ? "LESSON_COMPLETED" : "PROGRESS",
					Boolean.TRUE.equals(progress.getCompleted()) ? "Lesson completed" : "Learning progress updated",
					"Lesson #" + progress.getLessonId(),
					coalesce(progress.getCompletedAt(), progress.getUpdatedAt())
				)));
		}

		return activities.stream()
			.filter(activity -> activity.occurredAt() != null)
			.sorted(Comparator.comparing(DashboardActivityResponse::occurredAt).reversed())
			.limit(RECENT_LIMIT)
			.toList();
	}

	private DashboardCourseResponse toCourseResponse(Course course) {
		return new DashboardCourseResponse(
			course.getId(),
			course.getTitle(),
			course.getSubtitle(),
			course.getCoverFileId(),
			course.getOwnerId(),
			course.getStatus(),
			course.getPublishedAt(),
			course.getCreatedAt(),
			lessonRepository.countByCourseId(course.getId()),
			courseEnrollmentRepository.countByCourseId(course.getId())
		);
	}

	private DashboardLearningResponse toLearningResponse(CourseEnrollment enrollment, Course course) {
		if (course == null) {
			return null;
		}

		long totalLessons = lessonRepository.countByCourseIdAndStatus(course.getId(), LessonStatus.PUBLISHED);
		long completedLessons = lessonProgressRepository.countCompletedLessonsByUserIdAndCourseIdAndLessonStatus(
			enrollment.getUserId(),
			course.getId(),
			LessonStatus.PUBLISHED
		);
		Instant lastStudiedAt = lessonProgressRepository
			.findTopByUserIdAndCourseIdOrderByUpdatedAtDesc(enrollment.getUserId(), course.getId())
			.map(LessonProgress::getUpdatedAt)
			.orElse(null);

		return new DashboardLearningResponse(
			enrollment.getId(),
			course.getId(),
			course.getTitle(),
			course.getSubtitle(),
			course.getCoverFileId(),
			course.getStatus(),
			enrollment.getEnrolledAt(),
			totalLessons,
			completedLessons,
			totalLessons == 0 ? 0 : (double) completedLessons / totalLessons,
			lastStudiedAt
		);
	}

	private DashboardMediaResponse toMediaResponse(FileAsset fileAsset) {
		return new DashboardMediaResponse(
			fileAsset.getId(),
			fileAsset.getOriginalName(),
			fileAsset.getContentType(),
			fileAsset.getSizeBytes(),
			fileAsset.getAssetType(),
			fileAsset.getStorageProvider(),
			fileAsset.getCreatedAt()
		);
	}

	private List<Long> getManagedCourseIds(AuthenticatedUser currentUser, boolean admin, boolean canManageCourses) {
		if (!canManageCourses) {
			return List.of();
		}

		if (admin) {
			return courseRepository.findAllCourseIds();
		}

		return courseRepository.findCourseIdsByOwnerId(currentUser.getId());
	}

	private List<Long> getLearningCourseIds(AuthenticatedUser currentUser, boolean student) {
		if (!student) {
			return List.of();
		}

		return courseEnrollmentRepository.findCourseIdsByUserId(currentUser.getId());
	}

	private long countLessons(Collection<Long> courseIds) {
		return courseIds.isEmpty() ? 0 : lessonRepository.countByCourseIdIn(courseIds);
	}

	private long countEnrollments(Collection<Long> courseIds) {
		return courseIds.isEmpty() ? 0 : courseEnrollmentRepository.countByCourseIdIn(courseIds);
	}

	private double calculateManagedCompletionRate(List<Long> courseIds, long totalLessons, long totalEnrollments) {
		if (courseIds.isEmpty() || totalLessons == 0 || totalEnrollments == 0) {
			return 0;
		}

		long completedLessons = lessonProgressRepository.countByCourseIdInAndCompletedTrue(courseIds);
		long possibleCompletions = totalLessons * totalEnrollments;

		return possibleCompletions == 0 ? 0 : (double) completedLessons / possibleCompletions;
	}

	private boolean hasCourseAuthority(AuthenticatedUser user) {
		return user.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.anyMatch(authority -> authority.startsWith("course:"));
	}

	private boolean hasRole(AuthenticatedUser user, String role) {
		return user.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.anyMatch(authority -> authority.equals("ROLE_" + role));
	}

	private Instant coalesce(Instant first, Instant second) {
		return first == null ? second : first;
	}
}
