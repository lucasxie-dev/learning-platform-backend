package dev.lucasxie.learning.progress.service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.lucasxie.learning.auth.security.AuthenticatedUser;
import dev.lucasxie.learning.auth.security.SecurityUtils;
import dev.lucasxie.learning.common.api.PageResponse;
import dev.lucasxie.learning.common.exception.BusinessException;
import dev.lucasxie.learning.common.exception.ErrorCode;
import dev.lucasxie.learning.course.Course;
import dev.lucasxie.learning.course.CourseEnrollment;
import dev.lucasxie.learning.course.CourseEnrollmentRepository;
import dev.lucasxie.learning.course.CourseRepository;
import dev.lucasxie.learning.course.CourseStatus;
import dev.lucasxie.learning.lesson.LessonProgressRepository;
import dev.lucasxie.learning.lesson.LessonRepository;
import dev.lucasxie.learning.lesson.LessonStatus;
import dev.lucasxie.learning.progress.dto.EnrollmentResponse;
import dev.lucasxie.learning.progress.dto.MyCourseResponse;

@Service
public class EnrollmentServiceImpl implements EnrollmentService {

	private static final int MAX_PAGE_SIZE = 100;

	private final CourseEnrollmentRepository courseEnrollmentRepository;

	private final CourseRepository courseRepository;

	private final LessonRepository lessonRepository;

	private final LessonProgressRepository lessonProgressRepository;

	public EnrollmentServiceImpl(
		CourseEnrollmentRepository courseEnrollmentRepository,
		CourseRepository courseRepository,
		LessonRepository lessonRepository,
		LessonProgressRepository lessonProgressRepository
	) {
		this.courseEnrollmentRepository = courseEnrollmentRepository;
		this.courseRepository = courseRepository;
		this.lessonRepository = lessonRepository;
		this.lessonProgressRepository = lessonProgressRepository;
	}

	@Override
	@Transactional
	public EnrollmentResponse enrollCourse(Long courseId) {
		AuthenticatedUser currentUser = requireCurrentUser();
		requireStudent(currentUser, "Only students can enroll in courses");
		Course course = getExistingCourse(courseId);

		if (course.getStatus() != CourseStatus.PUBLISHED) {
			throw new BusinessException(ErrorCode.COURSE_INVALID_STATUS, "Only published courses can be enrolled");
		}

		if (courseEnrollmentRepository.existsByUserIdAndCourseId(currentUser.getId(), courseId)) {
			throw new BusinessException(ErrorCode.ENROLLMENT_ALREADY_EXISTS, "You are already enrolled in this course");
		}

		CourseEnrollment enrollment = new CourseEnrollment();
		enrollment.setUserId(currentUser.getId());
		enrollment.setCourseId(course.getId());
		enrollment.setEnrolledAt(Instant.now());

		return toEnrollmentResponse(courseEnrollmentRepository.save(enrollment), course);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<MyCourseResponse> listMyCourses(int page, int size) {
		AuthenticatedUser currentUser = requireCurrentUser();
		Page<CourseEnrollment> enrollments = courseEnrollmentRepository.findByUserId(
			currentUser.getId(),
			pageable(page, size, Sort.by(Sort.Direction.DESC, "enrolledAt"))
		);
		List<MyCourseResponse> items = enrollments.getContent()
			.stream()
			.map(this::toMyCourseResponse)
			.toList();

		return toPageResponse(items, enrollments);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<EnrollmentResponse> listCourseEnrollments(Long courseId, int page, int size) {
		Course course = getExistingCourse(courseId);
		AuthenticatedUser currentUser = requireCurrentUser();
		requireEnrollmentViewAccess(currentUser, course);

		Page<CourseEnrollment> enrollments = courseEnrollmentRepository.findByCourseId(
			courseId,
			pageable(page, size, Sort.by(Sort.Direction.DESC, "enrolledAt"))
		);
		List<EnrollmentResponse> items = enrollments.getContent()
			.stream()
			.map(enrollment -> toEnrollmentResponse(enrollment, course))
			.toList();

		return toPageResponse(items, enrollments);
	}

	private MyCourseResponse toMyCourseResponse(CourseEnrollment enrollment) {
		Course course = getExistingCourse(enrollment.getCourseId());
		long totalLessons = lessonRepository.countByCourseIdAndStatus(course.getId(), LessonStatus.PUBLISHED);
		long completedLessons = lessonProgressRepository.countCompletedLessonsByUserIdAndCourseIdAndLessonStatus(
			enrollment.getUserId(),
			course.getId(),
			LessonStatus.PUBLISHED
		);
		Instant lastStudiedAt = lessonProgressRepository
			.findTopByUserIdAndCourseIdOrderByUpdatedAtDesc(enrollment.getUserId(), course.getId())
			.map(progress -> progress.getUpdatedAt() != null ? progress.getUpdatedAt() : progress.getCreatedAt())
			.orElse(null);

		return new MyCourseResponse(
			enrollment.getId(),
			course.getId(),
			course.getTitle(),
			course.getSubtitle(),
			course.getCoverFileId(),
			course.getStatus(),
			enrollment.getEnrolledAt(),
			totalLessons,
			completedLessons,
			completionRate(completedLessons, totalLessons),
			lastStudiedAt
		);
	}

	private EnrollmentResponse toEnrollmentResponse(CourseEnrollment enrollment, Course course) {
		return new EnrollmentResponse(
			enrollment.getId(),
			enrollment.getUserId(),
			enrollment.getCourseId(),
			enrollment.getEnrolledAt(),
			course.getTitle(),
			course.getStatus()
		);
	}

	private Course getExistingCourse(Long courseId) {
		return courseRepository.findById(courseId)
			.orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND, "Course was not found"));
	}

	private AuthenticatedUser requireCurrentUser() {
		return SecurityUtils.getCurrentUser()
			.orElseThrow(() -> new BusinessException(ErrorCode.COMMON_UNAUTHORIZED, "Authentication is required"));
	}

	private void requireStudent(AuthenticatedUser currentUser, String message) {
		if (!hasRole(currentUser, "STUDENT")) {
			throw new BusinessException(ErrorCode.ENROLLMENT_ACCESS_DENIED, message);
		}
	}

	private void requireEnrollmentViewAccess(AuthenticatedUser currentUser, Course course) {
		if (hasRole(currentUser, "ADMIN")) {
			return;
		}

		if (hasRole(currentUser, "TEACHER") && course.getOwnerId().equals(currentUser.getId())) {
			return;
		}

		throw new BusinessException(ErrorCode.ENROLLMENT_ACCESS_DENIED, "You do not have access to course enrollments");
	}

	private boolean hasRole(AuthenticatedUser user, String role) {
		return hasAuthority(user, "ROLE_" + role);
	}

	private boolean hasAuthority(AuthenticatedUser user, String authority) {
		Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
		return authorities.stream().anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
	}

	private Pageable pageable(int page, int size, Sort sort) {
		return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sort);
	}

	private double completionRate(long completedLessons, long totalLessons) {
		if (totalLessons == 0) {
			return 0.0d;
		}

		return (double) completedLessons / totalLessons;
	}

	private <T> PageResponse<T> toPageResponse(List<T> items, Page<?> page) {
		return new PageResponse<>(items, page.getTotalElements(), page.getNumber(), page.getSize(), page.getTotalPages());
	}
}
