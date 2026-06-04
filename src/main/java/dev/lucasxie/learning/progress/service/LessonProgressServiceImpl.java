package dev.lucasxie.learning.progress.service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.lucasxie.learning.auth.security.AuthenticatedUser;
import dev.lucasxie.learning.auth.security.SecurityUtils;
import dev.lucasxie.learning.common.exception.BusinessException;
import dev.lucasxie.learning.common.exception.ErrorCode;
import dev.lucasxie.learning.course.Course;
import dev.lucasxie.learning.course.CourseEnrollmentRepository;
import dev.lucasxie.learning.course.CourseRepository;
import dev.lucasxie.learning.course.CourseStatus;
import dev.lucasxie.learning.lesson.Lesson;
import dev.lucasxie.learning.lesson.LessonProgress;
import dev.lucasxie.learning.lesson.LessonProgressRepository;
import dev.lucasxie.learning.lesson.LessonRepository;
import dev.lucasxie.learning.lesson.LessonStatus;
import dev.lucasxie.learning.progress.dto.CourseProgressResponse;
import dev.lucasxie.learning.progress.dto.LessonProgressItemResponse;
import dev.lucasxie.learning.progress.dto.LessonProgressResponse;
import dev.lucasxie.learning.progress.dto.LessonProgressUpdateRequest;

@Service
public class LessonProgressServiceImpl implements LessonProgressService {

	private final LessonProgressRepository lessonProgressRepository;

	private final LessonRepository lessonRepository;

	private final CourseRepository courseRepository;

	private final CourseEnrollmentRepository courseEnrollmentRepository;

	public LessonProgressServiceImpl(
		LessonProgressRepository lessonProgressRepository,
		LessonRepository lessonRepository,
		CourseRepository courseRepository,
		CourseEnrollmentRepository courseEnrollmentRepository
	) {
		this.lessonProgressRepository = lessonProgressRepository;
		this.lessonRepository = lessonRepository;
		this.courseRepository = courseRepository;
		this.courseEnrollmentRepository = courseEnrollmentRepository;
	}

	@Override
	@Transactional
	public LessonProgressResponse updateLessonProgress(Long lessonId, LessonProgressUpdateRequest request) {
		AuthenticatedUser currentUser = requireCurrentUser();
		requireStudent(currentUser);
		Lesson lesson = getExistingLesson(lessonId);
		Course course = getExistingCourse(lesson.getCourseId());
		requirePublishedLearningAccess(currentUser, course, lesson);
		validateProgressSeconds(request.progressSeconds(), lesson);

		LessonProgress progress = getOrCreateProgress(currentUser.getId(), course.getId(), lesson.getId());
		progress.setProgressSeconds(request.progressSeconds());
		if (hasPositiveDuration(lesson) && request.progressSeconds() >= lesson.getDurationSeconds()) {
			complete(progress, lesson);
		}

		return toResponse(lessonProgressRepository.save(progress));
	}

	@Override
	@Transactional
	public LessonProgressResponse completeLesson(Long lessonId) {
		AuthenticatedUser currentUser = requireCurrentUser();
		requireStudent(currentUser);
		Lesson lesson = getExistingLesson(lessonId);
		Course course = getExistingCourse(lesson.getCourseId());
		requirePublishedLearningAccess(currentUser, course, lesson);

		LessonProgress progress = getOrCreateProgress(currentUser.getId(), course.getId(), lesson.getId());
		complete(progress, lesson);

		return toResponse(lessonProgressRepository.save(progress));
	}

	@Override
	@Transactional(readOnly = true)
	public CourseProgressResponse getMyCourseProgress(Long courseId) {
		AuthenticatedUser currentUser = requireCurrentUser();
		Course course = getExistingCourse(courseId);
		if (!courseEnrollmentRepository.existsByUserIdAndCourseId(currentUser.getId(), courseId)) {
			throw new BusinessException(ErrorCode.PROGRESS_ACCESS_DENIED, "You are not enrolled in this course");
		}

		if (course.getStatus() != CourseStatus.PUBLISHED) {
			throw new BusinessException(ErrorCode.COURSE_INVALID_STATUS, "Course progress is available only for published courses");
		}

		return buildCourseProgress(course, currentUser.getId());
	}

	@Override
	@Transactional(readOnly = true)
	public CourseProgressResponse getUserCourseProgress(Long courseId, Long userId) {
		AuthenticatedUser currentUser = requireCurrentUser();
		Course course = getExistingCourse(courseId);
		requireProgressViewAccess(currentUser, course);

		if (!courseEnrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
			throw new BusinessException(ErrorCode.ENROLLMENT_NOT_FOUND, "User is not enrolled in this course");
		}

		return buildCourseProgress(course, userId);
	}

	private CourseProgressResponse buildCourseProgress(Course course, Long userId) {
		List<Lesson> lessons = lessonRepository.findByCourseIdAndStatusOrderBySortOrderAscIdAsc(
			course.getId(),
			LessonStatus.PUBLISHED
		);
		Map<Long, LessonProgress> progressByLessonId = lessonProgressRepository
			.findByUserIdAndCourseId(userId, course.getId())
			.stream()
			.collect(Collectors.toMap(LessonProgress::getLessonId, Function.identity(), (left, right) -> left));

		List<LessonProgressItemResponse> items = lessons.stream()
			.map(lesson -> toItemResponse(lesson, progressByLessonId.get(lesson.getId())))
			.toList();
		long completedLessons = items.stream()
			.filter(item -> Boolean.TRUE.equals(item.completed()))
			.count();

		return new CourseProgressResponse(
			course.getId(),
			userId,
			lessons.size(),
			completedLessons,
			completionRate(completedLessons, lessons.size()),
			items
		);
	}

	private LessonProgressItemResponse toItemResponse(Lesson lesson, LessonProgress progress) {
		return new LessonProgressItemResponse(
			lesson.getId(),
			lesson.getTitle(),
			lesson.getSortOrder(),
			lesson.getDurationSeconds(),
			progress == null ? 0 : progress.getProgressSeconds(),
			progress != null && Boolean.TRUE.equals(progress.getCompleted()),
			progress == null ? null : progress.getCompletedAt(),
			progress == null ? null : progress.getUpdatedAt()
		);
	}

	private LessonProgress getOrCreateProgress(Long userId, Long courseId, Long lessonId) {
		return lessonProgressRepository.findByUserIdAndLessonId(userId, lessonId)
			.orElseGet(() -> {
				LessonProgress progress = new LessonProgress();
				progress.setUserId(userId);
				progress.setCourseId(courseId);
				progress.setLessonId(lessonId);
				progress.setProgressSeconds(0);
				progress.setCompleted(false);
				return progress;
			});
	}

	private void complete(LessonProgress progress, Lesson lesson) {
		if (hasPositiveDuration(lesson) && progress.getProgressSeconds() < lesson.getDurationSeconds()) {
			progress.setProgressSeconds(lesson.getDurationSeconds());
		}

		if (!Boolean.TRUE.equals(progress.getCompleted())) {
			progress.setCompleted(true);
			progress.setCompletedAt(Instant.now());
		}
	}

	private void validateProgressSeconds(Integer progressSeconds, Lesson lesson) {
		if (progressSeconds == null || progressSeconds < 0) {
			throw new BusinessException(ErrorCode.PROGRESS_INVALID_VALUE, "Progress seconds must be non-negative");
		}

		if (hasPositiveDuration(lesson) && progressSeconds > lesson.getDurationSeconds()) {
			throw new BusinessException(ErrorCode.PROGRESS_INVALID_VALUE, "Progress seconds cannot exceed lesson duration");
		}
	}

	private void requirePublishedLearningAccess(AuthenticatedUser currentUser, Course course, Lesson lesson) {
		if (course.getStatus() != CourseStatus.PUBLISHED) {
			throw new BusinessException(ErrorCode.COURSE_INVALID_STATUS, "Course must be published to update progress");
		}

		if (lesson.getStatus() != LessonStatus.PUBLISHED) {
			throw new BusinessException(ErrorCode.LESSON_INVALID_STATUS, "Lesson must be published to update progress");
		}

		if (!courseEnrollmentRepository.existsByUserIdAndCourseId(currentUser.getId(), course.getId())) {
			throw new BusinessException(ErrorCode.PROGRESS_ACCESS_DENIED, "You are not enrolled in this course");
		}
	}

	private void requireProgressViewAccess(AuthenticatedUser currentUser, Course course) {
		if (hasRole(currentUser, "ADMIN")) {
			return;
		}

		if (hasRole(currentUser, "TEACHER") && course.getOwnerId().equals(currentUser.getId())) {
			return;
		}

		throw new BusinessException(ErrorCode.PROGRESS_ACCESS_DENIED, "You do not have access to this user's progress");
	}

	private void requireStudent(AuthenticatedUser currentUser) {
		if (!hasRole(currentUser, "STUDENT")) {
			throw new BusinessException(ErrorCode.PROGRESS_ACCESS_DENIED, "Only students can update lesson progress");
		}
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

	private boolean hasPositiveDuration(Lesson lesson) {
		return lesson.getDurationSeconds() != null && lesson.getDurationSeconds() > 0;
	}

	private boolean hasRole(AuthenticatedUser user, String role) {
		return hasAuthority(user, "ROLE_" + role);
	}

	private boolean hasAuthority(AuthenticatedUser user, String authority) {
		Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
		return authorities.stream().anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
	}

	private double completionRate(long completedLessons, long totalLessons) {
		if (totalLessons == 0) {
			return 0.0d;
		}

		return (double) completedLessons / totalLessons;
	}

	private LessonProgressResponse toResponse(LessonProgress progress) {
		return new LessonProgressResponse(
			progress.getId(),
			progress.getUserId(),
			progress.getCourseId(),
			progress.getLessonId(),
			progress.getProgressSeconds(),
			progress.getCompleted(),
			progress.getCompletedAt(),
			progress.getCreatedAt(),
			progress.getUpdatedAt()
		);
	}
}
