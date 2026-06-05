package dev.lucasxie.learning.lesson.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import dev.lucasxie.learning.auth.security.AuthenticatedUser;
import dev.lucasxie.learning.common.exception.BusinessException;
import dev.lucasxie.learning.common.exception.ErrorCode;
import dev.lucasxie.learning.course.Course;
import dev.lucasxie.learning.course.CourseRepository;
import dev.lucasxie.learning.course.CourseStatus;
import dev.lucasxie.learning.lesson.Lesson;
import dev.lucasxie.learning.lesson.LessonRepository;
import dev.lucasxie.learning.lesson.LessonStatus;
import dev.lucasxie.learning.lesson.dto.LessonCreateRequest;
import dev.lucasxie.learning.lesson.dto.LessonOrderItem;
import dev.lucasxie.learning.lesson.dto.LessonReorderRequest;
import dev.lucasxie.learning.lesson.mapper.LessonMapper;
import dev.lucasxie.learning.user.UserStatus;

class LessonServiceImplTest {

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void adminCanManageLessonsUnderAnyCourse() {
		FakeCourseRepository courses = new FakeCourseRepository(course(1L, 2L, CourseStatus.PUBLISHED));
		FakeLessonRepository lessons = new FakeLessonRepository(lesson(10L, 1L, LessonStatus.DRAFT, 4));
		LessonServiceImpl service = createService(courses.repository(), lessons.repository());
		authenticate(admin(1L));

		var response = service.createLesson(1L, new LessonCreateRequest("Intro", "Start here", null, null));

		assertThat(response.courseId()).isEqualTo(1L);
		assertThat(response.status()).isEqualTo(LessonStatus.DRAFT);
		assertThat(response.sortOrder()).isEqualTo(5);
		assertThat(lessons.count()).isEqualTo(2);
	}

	@Test
	void teacherCanCreateLessonUnderOwnedCourse() {
		FakeCourseRepository courses = new FakeCourseRepository(course(1L, 10L, CourseStatus.DRAFT));
		FakeLessonRepository lessons = new FakeLessonRepository();
		LessonServiceImpl service = createService(courses.repository(), lessons.repository());
		authenticate(teacher(10L));

		var response = service.createLesson(1L, new LessonCreateRequest("Draft lesson", null, null, 0));

		assertThat(response.courseId()).isEqualTo(1L);
		assertThat(response.sortOrder()).isZero();
		assertThat(lessons.count()).isEqualTo(1);
	}

	@Test
	void teacherCannotCreateLessonUnderAnotherTeachersCourse() {
		FakeCourseRepository courses = new FakeCourseRepository(course(1L, 99L, CourseStatus.DRAFT));
		FakeLessonRepository lessons = new FakeLessonRepository();
		LessonServiceImpl service = createService(courses.repository(), lessons.repository());
		authenticate(teacher(10L));

		assertThatThrownBy(() -> service.createLesson(1L, new LessonCreateRequest("Blocked", null, null, null)))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.LESSON_ACCESS_DENIED.name());
		assertThat(lessons.count()).isZero();
	}

	@Test
	void studentCannotCreateLesson() {
		FakeCourseRepository courses = new FakeCourseRepository(course(1L, 10L, CourseStatus.PUBLISHED));
		FakeLessonRepository lessons = new FakeLessonRepository();
		LessonServiceImpl service = createService(courses.repository(), lessons.repository());
		authenticate(student(20L));

		assertThatThrownBy(() -> service.createLesson(1L, new LessonCreateRequest("Blocked", null, null, null)))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.LESSON_ACCESS_DENIED.name());
		assertThat(lessons.count()).isZero();
	}

	@Test
	void studentCanViewPublishedLessonUnderPublishedCourse() {
		FakeCourseRepository courses = new FakeCourseRepository(course(2L, 10L, CourseStatus.PUBLISHED));
		FakeLessonRepository lessons = new FakeLessonRepository(lesson(1L, 2L, LessonStatus.PUBLISHED, 0));
		LessonServiceImpl service = createService(courses.repository(), lessons.repository());
		authenticate(student(20L));

		var response = service.getLesson(1L);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.status()).isEqualTo(LessonStatus.PUBLISHED);
	}

	@Test
	void studentCannotViewDraftLesson() {
		FakeCourseRepository courses = new FakeCourseRepository(course(2L, 10L, CourseStatus.PUBLISHED));
		FakeLessonRepository lessons = new FakeLessonRepository(lesson(1L, 2L, LessonStatus.DRAFT, 0));
		LessonServiceImpl service = createService(courses.repository(), lessons.repository());
		authenticate(student(20L));

		assertThatThrownBy(() -> service.getLesson(1L))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.LESSON_ACCESS_DENIED.name());
	}

	@Test
	void lessonCannotBePublishedIfParentCourseIsNotPublished() {
		FakeCourseRepository courses = new FakeCourseRepository(course(2L, 10L, CourseStatus.DRAFT));
		Lesson lesson = lesson(1L, 2L, LessonStatus.DRAFT, 0);
		FakeLessonRepository lessons = new FakeLessonRepository(lesson);
		LessonServiceImpl service = createService(courses.repository(), lessons.repository());
		authenticate(teacher(10L));

		assertThatThrownBy(() -> service.publishLesson(1L))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.LESSON_INVALID_STATUS.name());
		assertThat(lesson.getStatus()).isEqualTo(LessonStatus.DRAFT);
	}

	@Test
	void lessonPublishChangesStatusToPublished() {
		FakeCourseRepository courses = new FakeCourseRepository(course(2L, 10L, CourseStatus.PUBLISHED));
		Lesson lesson = lesson(1L, 2L, LessonStatus.DRAFT, 0);
		FakeLessonRepository lessons = new FakeLessonRepository(lesson);
		LessonServiceImpl service = createService(courses.repository(), lessons.repository());
		authenticate(teacher(10L));

		var response = service.publishLesson(1L);

		assertThat(response.status()).isEqualTo(LessonStatus.PUBLISHED);
		assertThat(lesson.getStatus()).isEqualTo(LessonStatus.PUBLISHED);
	}

	@Test
	void lessonArchiveChangesStatusToArchived() {
		FakeCourseRepository courses = new FakeCourseRepository(course(2L, 10L, CourseStatus.PUBLISHED));
		Lesson lesson = lesson(1L, 2L, LessonStatus.PUBLISHED, 0);
		FakeLessonRepository lessons = new FakeLessonRepository(lesson);
		LessonServiceImpl service = createService(courses.repository(), lessons.repository());
		authenticate(admin(1L));

		var response = service.archiveLesson(1L);

		assertThat(response.status()).isEqualTo(LessonStatus.ARCHIVED);
		assertThat(lesson.getStatus()).isEqualTo(LessonStatus.ARCHIVED);
	}

	@Test
	void reorderRejectsLessonIdsThatDoNotBelongToCourse() {
		FakeCourseRepository courses = new FakeCourseRepository(course(2L, 10L, CourseStatus.PUBLISHED));
		FakeLessonRepository lessons = new FakeLessonRepository(
			lesson(1L, 2L, LessonStatus.DRAFT, 0),
			lesson(99L, 3L, LessonStatus.DRAFT, 0)
		);
		LessonServiceImpl service = createService(courses.repository(), lessons.repository());
		authenticate(teacher(10L));
		LessonReorderRequest request = new LessonReorderRequest(List.of(
			new LessonOrderItem(1L, 1),
			new LessonOrderItem(99L, 2)
		));

		assertThatThrownBy(() -> service.reorderLessons(2L, request))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.LESSON_INVALID_ORDER.name());
	}

	private LessonServiceImpl createService(CourseRepository courseRepository, LessonRepository lessonRepository) {
		return new LessonServiceImpl(lessonRepository, courseRepository, new LessonMapper());
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

	private Course course(Long id, Long ownerId, CourseStatus status) {
		Course course = new Course();
		course.setId(id);
		course.setOwnerId(ownerId);
		course.setTitle("Course %s".formatted(id));
		course.setStatus(status);
		return course;
	}

	private Lesson lesson(Long id, Long courseId, LessonStatus status, Integer sortOrder) {
		Lesson lesson = new Lesson();
		lesson.setId(id);
		lesson.setCourseId(courseId);
		lesson.setTitle("Lesson %s".formatted(id));
		lesson.setStatus(status);
		lesson.setSortOrder(sortOrder);
		return lesson;
	}

	private static final class FakeCourseRepository {

		private final Map<Long, Course> courses = new LinkedHashMap<>();

		private FakeCourseRepository(Course... courses) {
			for (Course course : courses) {
				this.courses.put(course.getId(), course);
			}
		}

		private CourseRepository repository() {
			return (CourseRepository) Proxy.newProxyInstance(
				CourseRepository.class.getClassLoader(),
				new Class<?>[] {CourseRepository.class},
				(proxy, method, args) -> switch (method.getName()) {
					case "findById" -> Optional.ofNullable(courses.get((Long) args[0]));
					case "toString" -> "FakeCourseRepository";
					default -> throw new UnsupportedOperationException(method.getName());
				}
			);
		}
	}

	private static final class FakeLessonRepository {

		private final Map<Long, Lesson> lessons = new LinkedHashMap<>();

		private long nextId = 1;

		private FakeLessonRepository(Lesson... lessons) {
			for (Lesson lesson : lessons) {
				this.lessons.put(lesson.getId(), lesson);
				this.nextId = Math.max(this.nextId, lesson.getId() + 1);
			}
		}

		private int count() {
			return lessons.size();
		}

		private LessonRepository repository() {
			return (LessonRepository) Proxy.newProxyInstance(
				LessonRepository.class.getClassLoader(),
				new Class<?>[] {LessonRepository.class},
				(proxy, method, args) -> switch (method.getName()) {
					case "findById" -> Optional.ofNullable(lessons.get((Long) args[0]));
					case "save" -> save((Lesson) args[0]);
					case "findMaxSortOrderByCourseId" -> findMaxSortOrderByCourseId((Long) args[0]);
					case "findByCourseIdAndIdIn" -> findByCourseIdAndIdIn((Long) args[0], castIds(args[1]));
					case "findByCourseIdOrderBySortOrderAscIdAsc" -> findByCourseIdOrderBySortOrderAscIdAsc((Long) args[0]);
					case "findByCourseIdAndStatusOrderBySortOrderAscIdAsc" ->
						findByCourseIdAndStatusOrderBySortOrderAscIdAsc((Long) args[0], (LessonStatus) args[1]);
					case "toString" -> "FakeLessonRepository";
					default -> throw new UnsupportedOperationException(method.getName());
				}
			);
		}

		private Lesson save(Lesson lesson) {
			if (lesson.getId() == null) {
				lesson.setId(nextId++);
			}
			lessons.put(lesson.getId(), lesson);
			return lesson;
		}

		private Optional<Integer> findMaxSortOrderByCourseId(Long courseId) {
			return lessons.values()
				.stream()
				.filter(lesson -> courseId.equals(lesson.getCourseId()))
				.map(Lesson::getSortOrder)
				.max(Integer::compareTo);
		}

		private List<Lesson> findByCourseIdAndIdIn(Long courseId, Collection<Long> ids) {
			return lessons.values()
				.stream()
				.filter(lesson -> courseId.equals(lesson.getCourseId()))
				.filter(lesson -> ids.contains(lesson.getId()))
				.toList();
		}

		private List<Lesson> findByCourseIdOrderBySortOrderAscIdAsc(Long courseId) {
			return lessons.values()
				.stream()
				.filter(lesson -> courseId.equals(lesson.getCourseId()))
				.sorted(lessonComparator())
				.toList();
		}

		private List<Lesson> findByCourseIdAndStatusOrderBySortOrderAscIdAsc(Long courseId, LessonStatus status) {
			return lessons.values()
				.stream()
				.filter(lesson -> courseId.equals(lesson.getCourseId()))
				.filter(lesson -> status == lesson.getStatus())
				.sorted(lessonComparator())
				.toList();
		}

		@SuppressWarnings("unchecked")
		private Collection<Long> castIds(Object value) {
			return (Collection<Long>) value;
		}

		private Comparator<Lesson> lessonComparator() {
			return Comparator.comparing(Lesson::getSortOrder).thenComparing(Lesson::getId);
		}
	}
}
