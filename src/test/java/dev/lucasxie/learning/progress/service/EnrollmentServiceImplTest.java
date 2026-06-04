package dev.lucasxie.learning.progress.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.lucasxie.learning.auth.security.AuthenticatedUser;
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
import dev.lucasxie.learning.user.UserStatus;

import org.mockito.Mockito;

class EnrollmentServiceImplTest {

	private final CourseEnrollmentRepository enrollmentRepository = Mockito.mock(CourseEnrollmentRepository.class);

	private final CourseRepository courseRepository = Mockito.mock(CourseRepository.class);

	private final LessonRepository lessonRepository = Mockito.mock(LessonRepository.class);

	private final LessonProgressRepository lessonProgressRepository = Mockito.mock(LessonProgressRepository.class);

	private final EnrollmentServiceImpl service = new EnrollmentServiceImpl(
		enrollmentRepository,
		courseRepository,
		lessonRepository,
		lessonProgressRepository
	);

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void studentCanEnrollInPublishedCourse() {
		Course course = course(1L, 10L, CourseStatus.PUBLISHED);
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		when(enrollmentRepository.existsByUserIdAndCourseId(20L, 1L)).thenReturn(false);
		when(enrollmentRepository.save(any(CourseEnrollment.class))).thenAnswer(invocation -> {
			CourseEnrollment enrollment = invocation.getArgument(0);
			enrollment.setId(100L);
			return enrollment;
		});
		authenticate(user(20L, "ROLE_STUDENT"));

		var response = service.enrollCourse(1L);

		assertThat(response.id()).isEqualTo(100L);
		assertThat(response.userId()).isEqualTo(20L);
		assertThat(response.courseId()).isEqualTo(1L);
		assertThat(response.courseTitle()).isEqualTo("Course 1");
		assertThat(response.courseStatus()).isEqualTo(CourseStatus.PUBLISHED);
	}

	@Test
	void teacherCannotUseStudentEnrollmentEndpoint() {
		authenticate(user(10L, "ROLE_TEACHER"));

		assertThatThrownBy(() -> service.enrollCourse(1L))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.ENROLLMENT_ACCESS_DENIED.name());
	}

	@Test
	void duplicateEnrollmentIsRejected() {
		Course course = course(1L, 10L, CourseStatus.PUBLISHED);
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		when(enrollmentRepository.existsByUserIdAndCourseId(20L, 1L)).thenReturn(true);
		authenticate(user(20L, "ROLE_STUDENT"));

		assertThatThrownBy(() -> service.enrollCourse(1L))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.ENROLLMENT_ALREADY_EXISTS.name());
	}

	@Test
	void studentCannotEnrollInDraftCourse() {
		Course course = course(1L, 10L, CourseStatus.DRAFT);
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		authenticate(user(20L, "ROLE_STUDENT"));

		assertThatThrownBy(() -> service.enrollCourse(1L))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.COURSE_INVALID_STATUS.name());
	}

	@Test
	void teacherCanViewEnrollmentsForOwnedCourse() {
		Course course = course(1L, 10L, CourseStatus.PUBLISHED);
		CourseEnrollment enrollment = enrollment(100L, 20L, 1L);
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		when(enrollmentRepository.findByCourseId(any(Long.class), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(enrollment)));
		authenticate(user(10L, "ROLE_TEACHER"));

		var response = service.listCourseEnrollments(1L, 0, 20);

		assertThat(response.getItems()).hasSize(1);
		assertThat(response.getItems().getFirst().userId()).isEqualTo(20L);
		verify(enrollmentRepository).findByCourseId(any(Long.class), any(Pageable.class));
	}

	@Test
	void adminCanViewEnrollmentsForAnyCourse() {
		Course course = course(1L, 10L, CourseStatus.PUBLISHED);
		CourseEnrollment enrollment = enrollment(100L, 20L, 1L);
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		when(enrollmentRepository.findByCourseId(any(Long.class), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(enrollment)));
		authenticate(user(1L, "ROLE_ADMIN"));

		var response = service.listCourseEnrollments(1L, 0, 20);

		assertThat(response.getItems()).hasSize(1);
		assertThat(response.getItems().getFirst().courseId()).isEqualTo(1L);
	}

	@Test
	void listMyCoursesIncludesProgressSummary() {
		Course course = course(1L, 10L, CourseStatus.PUBLISHED);
		CourseEnrollment enrollment = enrollment(100L, 20L, 1L);
		when(enrollmentRepository.findByUserId(any(Long.class), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(enrollment)));
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		when(lessonRepository.countByCourseIdAndStatus(1L, LessonStatus.PUBLISHED)).thenReturn(4L);
		when(lessonProgressRepository.countCompletedLessonsByUserIdAndCourseIdAndLessonStatus(20L, 1L, LessonStatus.PUBLISHED))
			.thenReturn(3L);
		when(lessonProgressRepository.findTopByUserIdAndCourseIdOrderByUpdatedAtDesc(20L, 1L)).thenReturn(Optional.empty());
		authenticate(user(20L, "ROLE_STUDENT"));

		var response = service.listMyCourses(0, 20);

		assertThat(response.getItems()).hasSize(1);
		assertThat(response.getItems().getFirst().completedLessons()).isEqualTo(3L);
		assertThat(response.getItems().getFirst().completionRate()).isEqualTo(0.75d);
		verify(enrollmentRepository).findByUserId(eq(20L), any(Pageable.class));
	}

	private Course course(Long id, Long ownerId, CourseStatus status) {
		Course course = new Course();
		course.setId(id);
		course.setOwnerId(ownerId);
		course.setTitle("Course %s".formatted(id));
		course.setStatus(status);
		return course;
	}

	private CourseEnrollment enrollment(Long id, Long userId, Long courseId) {
		CourseEnrollment enrollment = new CourseEnrollment();
		enrollment.setId(id);
		enrollment.setUserId(userId);
		enrollment.setCourseId(courseId);
		enrollment.setEnrolledAt(Instant.parse("2026-01-01T00:00:00Z"));
		return enrollment;
	}

	private void authenticate(AuthenticatedUser user) {
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities())
		);
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
