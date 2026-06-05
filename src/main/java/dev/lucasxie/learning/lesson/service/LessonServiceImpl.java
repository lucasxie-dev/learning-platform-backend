package dev.lucasxie.learning.lesson.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import dev.lucasxie.learning.auth.security.AuthenticatedUser;
import dev.lucasxie.learning.auth.security.SecurityUtils;
import dev.lucasxie.learning.common.exception.BusinessException;
import dev.lucasxie.learning.common.exception.ErrorCode;
import dev.lucasxie.learning.course.Course;
import dev.lucasxie.learning.course.CourseRepository;
import dev.lucasxie.learning.course.CourseStatus;
import dev.lucasxie.learning.lesson.Lesson;
import dev.lucasxie.learning.lesson.LessonRepository;
import dev.lucasxie.learning.lesson.LessonStatus;
import dev.lucasxie.learning.lesson.dto.LessonCreateRequest;
import dev.lucasxie.learning.lesson.dto.LessonListItemResponse;
import dev.lucasxie.learning.lesson.dto.LessonOrderItem;
import dev.lucasxie.learning.lesson.dto.LessonReorderRequest;
import dev.lucasxie.learning.lesson.dto.LessonResponse;
import dev.lucasxie.learning.lesson.dto.LessonUpdateRequest;
import dev.lucasxie.learning.lesson.mapper.LessonMapper;

@Service
public class LessonServiceImpl implements LessonService {

	private final LessonRepository lessonRepository;

	private final CourseRepository courseRepository;

	private final LessonMapper lessonMapper;

	public LessonServiceImpl(
		LessonRepository lessonRepository,
		CourseRepository courseRepository,
		LessonMapper lessonMapper
	) {
		this.lessonRepository = lessonRepository;
		this.courseRepository = courseRepository;
		this.lessonMapper = lessonMapper;
	}

	@Override
	@Transactional
	public LessonResponse createLesson(Long courseId, LessonCreateRequest request) {
		Course course = getExistingCourse(courseId);
		AuthenticatedUser currentUser = requireCurrentUser();
		requireManageAccess(currentUser, course, "Only admins and course owners can create lessons");

		if (course.getStatus() == CourseStatus.ARCHIVED) {
			throw new BusinessException(ErrorCode.LESSON_INVALID_STATUS, "Cannot create lessons under archived courses");
		}

		Lesson lesson = new Lesson();
		lesson.setCourseId(course.getId());
		lesson.setTitle(request.title().trim());
		lesson.setDescription(normalizeOptionalText(request.description()));
		lesson.setContentMarkdown(normalizeOptionalText(request.contentMarkdown()));
		lesson.setSortOrder(resolveCreateSortOrder(course.getId(), request.sortOrder()));
		lesson.setStatus(LessonStatus.DRAFT);

		return lessonMapper.toResponse(lessonRepository.save(lesson));
	}

	@Override
	@Transactional
	public LessonResponse updateLesson(Long lessonId, LessonUpdateRequest request) {
		Lesson lesson = getExistingLesson(lessonId);
		Course course = getExistingCourse(lesson.getCourseId());
		AuthenticatedUser currentUser = requireCurrentUser();
		requireManageAccess(currentUser, course, "Only admins and course owners can update lessons");

		if (lesson.getStatus() == LessonStatus.PUBLISHED) {
			throw new BusinessException(ErrorCode.LESSON_INVALID_STATUS, "Published lessons cannot be updated");
		}

		lesson.setTitle(request.title().trim());
		lesson.setDescription(normalizeOptionalText(request.description()));
		lesson.setContentMarkdown(normalizeOptionalText(request.contentMarkdown()));
		if (request.sortOrder() != null) {
			lesson.setSortOrder(request.sortOrder());
		}

		return lessonMapper.toResponse(lesson);
	}

	@Override
	@Transactional(readOnly = true)
	public LessonResponse getLesson(Long lessonId) {
		Lesson lesson = getExistingLesson(lessonId);
		Course course = getExistingCourse(lesson.getCourseId());
		AuthenticatedUser currentUser = requireCurrentUser();
		requireViewAccess(currentUser, course, lesson);

		return lessonMapper.toResponse(lesson);
	}

	@Override
	@Transactional(readOnly = true)
	public List<LessonListItemResponse> listLessonsByCourse(Long courseId) {
		Course course = getExistingCourse(courseId);
		AuthenticatedUser currentUser = requireCurrentUser();
		List<Lesson> lessons;

		if (canViewAllLessons(currentUser, course)) {
			lessons = lessonRepository.findByCourseIdOrderBySortOrderAscIdAsc(courseId);
		}
		else {
			if (course.getStatus() != CourseStatus.PUBLISHED) {
				throw new BusinessException(ErrorCode.LESSON_ACCESS_DENIED, "You do not have access to these lessons");
			}
			lessons = lessonRepository.findByCourseIdAndStatusOrderBySortOrderAscIdAsc(courseId, LessonStatus.PUBLISHED);
		}

		return lessons.stream()
			.map(lessonMapper::toListItemResponse)
			.toList();
	}

	@Override
	@Transactional
	public LessonResponse publishLesson(Long lessonId) {
		Lesson lesson = getExistingLesson(lessonId);
		Course course = getExistingCourse(lesson.getCourseId());
		AuthenticatedUser currentUser = requireCurrentUser();
		requireManageAccess(currentUser, course, "Only admins and course owners can publish lessons");

		if (course.getStatus() != CourseStatus.PUBLISHED) {
			throw new BusinessException(ErrorCode.LESSON_INVALID_STATUS, "Lessons can only be published under published courses");
		}

		if (lesson.getStatus() == LessonStatus.PUBLISHED) {
			throw new BusinessException(ErrorCode.LESSON_INVALID_STATUS, "Lesson is already published");
		}

		lesson.setStatus(LessonStatus.PUBLISHED);

		return lessonMapper.toResponse(lesson);
	}

	@Override
	@Transactional
	public LessonResponse archiveLesson(Long lessonId) {
		Lesson lesson = getExistingLesson(lessonId);
		Course course = getExistingCourse(lesson.getCourseId());
		AuthenticatedUser currentUser = requireCurrentUser();
		requireManageAccess(currentUser, course, "Only admins and course owners can archive lessons");

		if (lesson.getStatus() != LessonStatus.PUBLISHED) {
			throw new BusinessException(ErrorCode.LESSON_INVALID_STATUS, "Only published lessons can be archived");
		}

		lesson.setStatus(LessonStatus.ARCHIVED);

		return lessonMapper.toResponse(lesson);
	}

	@Override
	@Transactional
	public void deleteLesson(Long lessonId) {
		Lesson lesson = getExistingLesson(lessonId);
		Course course = getExistingCourse(lesson.getCourseId());
		AuthenticatedUser currentUser = requireCurrentUser();
		requireManageAccess(currentUser, course, "Only admins and course owners can delete lessons");

		if (lesson.getStatus() != LessonStatus.ARCHIVED) {
			lesson.setStatus(LessonStatus.ARCHIVED);
		}
	}

	@Override
	@Transactional
	public void reorderLessons(Long courseId, LessonReorderRequest request) {
		Course course = getExistingCourse(courseId);
		AuthenticatedUser currentUser = requireCurrentUser();
		requireManageAccess(currentUser, course, "Only admins and course owners can reorder lessons");

		Set<Long> lessonIds = request.items()
			.stream()
			.map(LessonOrderItem::lessonId)
			.collect(Collectors.toCollection(HashSet::new));

		if (lessonIds.size() != request.items().size()) {
			throw new BusinessException(ErrorCode.LESSON_INVALID_ORDER, "Lesson reorder items must not contain duplicates");
		}

		List<Lesson> lessons = lessonRepository.findByCourseIdAndIdIn(courseId, lessonIds);
		if (lessons.size() != lessonIds.size()) {
			throw new BusinessException(ErrorCode.LESSON_INVALID_ORDER, "All reordered lessons must belong to the course");
		}

		Map<Long, Lesson> lessonsById = lessons.stream()
			.collect(Collectors.toMap(Lesson::getId, Function.identity()));

		for (LessonOrderItem item : request.items()) {
			lessonsById.get(item.lessonId()).setSortOrder(item.sortOrder());
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

	private void requireManageAccess(AuthenticatedUser currentUser, Course course, String message) {
		if (hasRole(currentUser, "ADMIN")) {
			return;
		}

		if (hasRole(currentUser, "TEACHER") && course.getOwnerId().equals(currentUser.getId())) {
			return;
		}

		throw new BusinessException(ErrorCode.LESSON_ACCESS_DENIED, message);
	}

	private void requireViewAccess(AuthenticatedUser currentUser, Course course, Lesson lesson) {
		if (canViewAllLessons(currentUser, course)) {
			return;
		}

		if (course.getStatus() == CourseStatus.PUBLISHED && lesson.getStatus() == LessonStatus.PUBLISHED) {
			return;
		}

		throw new BusinessException(ErrorCode.LESSON_ACCESS_DENIED, "You do not have access to this lesson");
	}

	private boolean canViewAllLessons(AuthenticatedUser currentUser, Course course) {
		if (hasRole(currentUser, "ADMIN")) {
			return true;
		}

		return hasRole(currentUser, "TEACHER") && course.getOwnerId().equals(currentUser.getId());
	}

	private boolean hasRole(AuthenticatedUser user, String role) {
		return hasAuthority(user, "ROLE_" + role);
	}

	private boolean hasAuthority(AuthenticatedUser user, String authority) {
		Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
		return authorities.stream().anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
	}

	private Integer resolveCreateSortOrder(Long courseId, Integer requestedSortOrder) {
		if (requestedSortOrder != null) {
			return requestedSortOrder;
		}

		return lessonRepository.findMaxSortOrderByCourseId(courseId)
			.map(sortOrder -> sortOrder + 1)
			.orElse(0);
	}

	private String normalizeOptionalText(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}

		return value.trim();
	}
}
