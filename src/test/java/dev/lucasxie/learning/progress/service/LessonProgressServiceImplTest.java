package dev.lucasxie.learning.progress.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.lucasxie.learning.auth.security.AuthenticatedUser;
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
import dev.lucasxie.learning.progress.dto.LessonProgressUpdateRequest;
import dev.lucasxie.learning.user.UserStatus;

class LessonProgressServiceImplTest {

	private final LessonProgressRepository progressRepository = Mockito.mock(LessonProgressRepository.class);

	private final LessonRepository lessonRepository = Mockito.mock(LessonRepository.class);

	private final CourseRepository courseRepository = Mockito.mock(CourseRepository.class);

	private final CourseEnrollmentRepository enrollmentRepository = Mockito.mock(CourseEnrollmentRepository.class);

	private final LessonProgressServiceImpl service = new LessonProgressServiceImpl(
		progressRepository,
		lessonRepository,
		courseRepository,
		enrollmentRepository
	);

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void studentCanUpdateProgressForEnrolledPublishedLesson() {
		Course course = course(1L, 10L, CourseStatus.PUBLISHED);
		Lesson lesson = lesson(2L, 1L, LessonStatus.PUBLISHED, 120);
		when(lessonRepository.findById(2L)).thenReturn(Optional.of(lesson));
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		when(enrollmentRepository.existsByUserIdAndCourseId(20L, 1L)).thenReturn(true);
		when(progressRepository.findByUserIdAndLessonId(20L, 2L)).thenReturn(Optional.empty());
		when(progressRepository.save(any(LessonProgress.class))).thenAnswer(invocation -> {
			LessonProgress progress = invocation.getArgument(0);
			progress.setId(200L);
			return progress;
		});
		authenticate(user(20L, "ROLE_STUDENT"));

		var response = service.updateLessonProgress(2L, new LessonProgressUpdateRequest(60));

		assertThat(response.id()).isEqualTo(200L);
		assertThat(response.progressSeconds()).isEqualTo(60);
		assertThat(response.completed()).isFalse();
	}

	@Test
	void progressSecondsCannotExceedPositiveLessonDuration() {
		Course course = course(1L, 10L, CourseStatus.PUBLISHED);
		Lesson lesson = lesson(2L, 1L, LessonStatus.PUBLISHED, 120);
		when(lessonRepository.findById(2L)).thenReturn(Optional.of(lesson));
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		when(enrollmentRepository.existsByUserIdAndCourseId(20L, 1L)).thenReturn(true);
		authenticate(user(20L, "ROLE_STUDENT"));

		assertThatThrownBy(() -> service.updateLessonProgress(2L, new LessonProgressUpdateRequest(121)))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.PROGRESS_INVALID_VALUE.name());
	}

	@Test
	void progressSecondsCannotBeNegative() {
		Course course = course(1L, 10L, CourseStatus.PUBLISHED);
		Lesson lesson = lesson(2L, 1L, LessonStatus.PUBLISHED, 120);
		when(lessonRepository.findById(2L)).thenReturn(Optional.of(lesson));
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		when(enrollmentRepository.existsByUserIdAndCourseId(20L, 1L)).thenReturn(true);
		authenticate(user(20L, "ROLE_STUDENT"));

		assertThatThrownBy(() -> service.updateLessonProgress(2L, new LessonProgressUpdateRequest(-1)))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.PROGRESS_INVALID_VALUE.name());
	}

	@Test
	void studentCannotUpdateProgressForDraftLesson() {
		Course course = course(1L, 10L, CourseStatus.PUBLISHED);
		Lesson lesson = lesson(2L, 1L, LessonStatus.DRAFT, 120);
		when(lessonRepository.findById(2L)).thenReturn(Optional.of(lesson));
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		authenticate(user(20L, "ROLE_STUDENT"));

		assertThatThrownBy(() -> service.updateLessonProgress(2L, new LessonProgressUpdateRequest(60)))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.LESSON_INVALID_STATUS.name());
	}

	@Test
	void studentCannotUpdateProgressForDraftCourse() {
		Course course = course(1L, 10L, CourseStatus.DRAFT);
		Lesson lesson = lesson(2L, 1L, LessonStatus.PUBLISHED, 120);
		when(lessonRepository.findById(2L)).thenReturn(Optional.of(lesson));
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		authenticate(user(20L, "ROLE_STUDENT"));

		assertThatThrownBy(() -> service.updateLessonProgress(2L, new LessonProgressUpdateRequest(60)))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.COURSE_INVALID_STATUS.name());
	}

	@Test
	void completingLessonSetsCompletionFieldsAndProgressToDuration() {
		Course course = course(1L, 10L, CourseStatus.PUBLISHED);
		Lesson lesson = lesson(2L, 1L, LessonStatus.PUBLISHED, 120);
		when(lessonRepository.findById(2L)).thenReturn(Optional.of(lesson));
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		when(enrollmentRepository.existsByUserIdAndCourseId(20L, 1L)).thenReturn(true);
		when(progressRepository.findByUserIdAndLessonId(20L, 2L)).thenReturn(Optional.empty());
		when(progressRepository.save(any(LessonProgress.class))).thenAnswer(invocation -> {
			LessonProgress progress = invocation.getArgument(0);
			progress.setId(200L);
			return progress;
		});
		authenticate(user(20L, "ROLE_STUDENT"));

		var response = service.completeLesson(2L);

		assertThat(response.completed()).isTrue();
		assertThat(response.completedAt()).isNotNull();
		assertThat(response.progressSeconds()).isEqualTo(120);
	}

	@Test
	void studentCannotUpdateProgressWithoutEnrollment() {
		Course course = course(1L, 10L, CourseStatus.PUBLISHED);
		Lesson lesson = lesson(2L, 1L, LessonStatus.PUBLISHED, 120);
		when(lessonRepository.findById(2L)).thenReturn(Optional.of(lesson));
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		when(enrollmentRepository.existsByUserIdAndCourseId(20L, 1L)).thenReturn(false);
		authenticate(user(20L, "ROLE_STUDENT"));

		assertThatThrownBy(() -> service.completeLesson(2L))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.PROGRESS_ACCESS_DENIED.name());
	}

	@Test
	void teacherCanViewOwnedCourseUserProgress() {
		Course course = course(1L, 10L, CourseStatus.PUBLISHED);
		Lesson first = lesson(2L, 1L, LessonStatus.PUBLISHED, 120);
		Lesson second = lesson(3L, 1L, LessonStatus.PUBLISHED, 90);
		LessonProgress completed = progress(20L, 1L, 2L, true);
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		when(enrollmentRepository.existsByUserIdAndCourseId(20L, 1L)).thenReturn(true);
		when(lessonRepository.findByCourseIdAndStatusOrderBySortOrderAscIdAsc(1L, LessonStatus.PUBLISHED))
			.thenReturn(List.of(first, second));
		when(progressRepository.findByUserIdAndCourseId(20L, 1L)).thenReturn(List.of(completed));
		authenticate(user(10L, "ROLE_TEACHER"));

		var response = service.getUserCourseProgress(1L, 20L);

		assertThat(response.totalLessons()).isEqualTo(2L);
		assertThat(response.completedLessons()).isEqualTo(1L);
		assertThat(response.completionRate()).isEqualTo(0.5d);
		assertThat(response.lessons()).hasSize(2);
	}

	@Test
	void teacherCannotViewAnotherTeachersCourseProgress() {
		Course course = course(1L, 99L, CourseStatus.PUBLISHED);
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		authenticate(user(10L, "ROLE_TEACHER"));

		assertThatThrownBy(() -> service.getUserCourseProgress(1L, 20L))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.PROGRESS_ACCESS_DENIED.name());
	}

	@Test
	void adminCanViewAnyCourseProgress() {
		Course course = course(1L, 10L, CourseStatus.PUBLISHED);
		Lesson first = lesson(2L, 1L, LessonStatus.PUBLISHED, 120);
		Lesson second = lesson(3L, 1L, LessonStatus.PUBLISHED, 90);
		LessonProgress completed = progress(20L, 1L, 2L, true);
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		when(enrollmentRepository.existsByUserIdAndCourseId(20L, 1L)).thenReturn(true);
		when(lessonRepository.findByCourseIdAndStatusOrderBySortOrderAscIdAsc(1L, LessonStatus.PUBLISHED))
			.thenReturn(List.of(first, second));
		when(progressRepository.findByUserIdAndCourseId(20L, 1L)).thenReturn(List.of(completed));
		authenticate(user(1L, "ROLE_ADMIN"));

		var response = service.getUserCourseProgress(1L, 20L);

		assertThat(response.userId()).isEqualTo(20L);
		assertThat(response.totalLessons()).isEqualTo(2L);
		assertThat(response.completedLessons()).isEqualTo(1L);
		assertThat(response.completionRate()).isEqualTo(0.5d);
	}

	@Test
	void studentCannotViewOtherUsersProgress() {
		Course course = course(1L, 10L, CourseStatus.PUBLISHED);
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		authenticate(user(20L, "ROLE_STUDENT"));

		assertThatThrownBy(() -> service.getUserCourseProgress(1L, 30L))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.PROGRESS_ACCESS_DENIED.name());
	}

	private Course course(Long id, Long ownerId, CourseStatus status) {
		Course course = new Course();
		course.setId(id);
		course.setOwnerId(ownerId);
		course.setTitle("Course %s".formatted(id));
		course.setStatus(status);
		return course;
	}

	private Lesson lesson(Long id, Long courseId, LessonStatus status, Integer durationSeconds) {
		Lesson lesson = new Lesson();
		lesson.setId(id);
		lesson.setCourseId(courseId);
		lesson.setTitle("Lesson %s".formatted(id));
		lesson.setStatus(status);
		lesson.setSortOrder(id.intValue());
		lesson.setDurationSeconds(durationSeconds);
		return lesson;
	}

	private LessonProgress progress(Long userId, Long courseId, Long lessonId, boolean completed) {
		LessonProgress progress = new LessonProgress();
		progress.setId(300L + lessonId);
		progress.setUserId(userId);
		progress.setCourseId(courseId);
		progress.setLessonId(lessonId);
		progress.setProgressSeconds(60);
		progress.setCompleted(completed);
		progress.setCompletedAt(completed ? Instant.parse("2026-01-01T00:00:00Z") : null);
		return progress;
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
