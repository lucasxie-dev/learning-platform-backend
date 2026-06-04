package dev.lucasxie.learning.course.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import dev.lucasxie.learning.auth.security.AuthenticatedUser;
import dev.lucasxie.learning.auth.security.SecurityUtils;
import dev.lucasxie.learning.common.api.PageResponse;
import dev.lucasxie.learning.common.exception.BusinessException;
import dev.lucasxie.learning.common.exception.ErrorCode;
import dev.lucasxie.learning.course.Course;
import dev.lucasxie.learning.course.CourseRepository;
import dev.lucasxie.learning.course.CourseStatus;
import dev.lucasxie.learning.course.dto.CourseCreateRequest;
import dev.lucasxie.learning.course.dto.CourseListItemResponse;
import dev.lucasxie.learning.course.dto.CourseResponse;
import dev.lucasxie.learning.course.dto.CourseUpdateRequest;
import dev.lucasxie.learning.course.mapper.CourseMapper;
import jakarta.persistence.criteria.Predicate;

@Service
public class CourseServiceImpl implements CourseService {

	private static final int MAX_PAGE_SIZE = 100;

	private final CourseRepository courseRepository;

	private final CourseMapper courseMapper;

	public CourseServiceImpl(CourseRepository courseRepository, CourseMapper courseMapper) {
		this.courseRepository = courseRepository;
		this.courseMapper = courseMapper;
	}

	@Override
	@Transactional
	public CourseResponse createCourse(CourseCreateRequest request) {
		AuthenticatedUser currentUser = requireCurrentUser();
		if (!hasRole(currentUser, "ADMIN") && !hasRole(currentUser, "TEACHER") && !hasAuthority(currentUser, "course:create")) {
			throw new BusinessException(ErrorCode.COURSE_ACCESS_DENIED, "Only admins and teachers can create courses");
		}

		Course course = new Course();
		course.setTitle(request.title().trim());
		course.setSubtitle(normalizeOptionalText(request.subtitle()));
		course.setDescription(normalizeOptionalText(request.description()));
		course.setOwnerId(currentUser.getId());
		course.setStatus(CourseStatus.DRAFT);

		return courseMapper.toResponse(courseRepository.save(course));
	}

	@Override
	@Transactional
	public CourseResponse updateCourse(Long courseId, CourseUpdateRequest request) {
		Course course = getExistingCourse(courseId);
		AuthenticatedUser currentUser = requireCurrentUser();
		requireManageAccess(currentUser, course, "Only admins and course owners can update courses");

		if (course.getStatus() == CourseStatus.PUBLISHED) {
			throw new BusinessException(ErrorCode.COURSE_INVALID_STATUS, "Published courses cannot be updated");
		}

		course.setTitle(request.title().trim());
		course.setSubtitle(normalizeOptionalText(request.subtitle()));
		course.setDescription(normalizeOptionalText(request.description()));

		return courseMapper.toResponse(course);
	}

	@Override
	@Transactional(readOnly = true)
	public CourseResponse getCourse(Long courseId) {
		Course course = getExistingCourse(courseId);
		AuthenticatedUser currentUser = requireCurrentUser();
		requireViewAccess(currentUser, course);

		return courseMapper.toResponse(course);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<CourseListItemResponse> listCourses(String keyword, CourseStatus status, int page, int size) {
		AuthenticatedUser currentUser = requireCurrentUser();
		Pageable pageable = PageRequest.of(
			Math.max(page, 0),
			Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
			Sort.by(Sort.Direction.DESC, "createdAt")
		);

		Page<Course> courses = courseRepository.findAll(buildCourseSpecification(currentUser, keyword, status), pageable);
		List<CourseListItemResponse> items = courses.getContent()
			.stream()
			.map(courseMapper::toListItemResponse)
			.toList();

		return new PageResponse<>(
			items,
			courses.getTotalElements(),
			courses.getNumber(),
			courses.getSize(),
			courses.getTotalPages()
		);
	}

	@Override
	@Transactional
	public CourseResponse publishCourse(Long courseId) {
		Course course = getExistingCourse(courseId);
		AuthenticatedUser currentUser = requireCurrentUser();
		requireManageAccess(currentUser, course, "Only admins and course owners can publish courses");

		if (course.getStatus() == CourseStatus.PUBLISHED) {
			throw new BusinessException(ErrorCode.COURSE_INVALID_STATUS, "Course is already published");
		}

		course.setStatus(CourseStatus.PUBLISHED);
		course.setPublishedAt(Instant.now());

		return courseMapper.toResponse(course);
	}

	@Override
	@Transactional
	public CourseResponse archiveCourse(Long courseId) {
		Course course = getExistingCourse(courseId);
		AuthenticatedUser currentUser = requireCurrentUser();
		requireManageAccess(currentUser, course, "Only admins and course owners can archive courses");

		if (course.getStatus() != CourseStatus.PUBLISHED) {
			throw new BusinessException(ErrorCode.COURSE_INVALID_STATUS, "Only published courses can be archived");
		}

		course.setStatus(CourseStatus.ARCHIVED);

		return courseMapper.toResponse(course);
	}

	@Override
	@Transactional
	public void deleteCourse(Long courseId) {
		Course course = getExistingCourse(courseId);
		AuthenticatedUser currentUser = requireCurrentUser();
		requireManageAccess(currentUser, course, "Only admins and course owners can delete courses");

		if (course.getStatus() != CourseStatus.ARCHIVED) {
			course.setStatus(CourseStatus.ARCHIVED);
		}
	}

	private Course getExistingCourse(Long courseId) {
		return courseRepository.findById(courseId)
			.orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND, "Course was not found"));
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

		throw new BusinessException(ErrorCode.COURSE_ACCESS_DENIED, message);
	}

	private void requireViewAccess(AuthenticatedUser currentUser, Course course) {
		if (hasRole(currentUser, "ADMIN")) {
			return;
		}

		if (course.getStatus() == CourseStatus.PUBLISHED) {
			return;
		}

		if (hasRole(currentUser, "TEACHER") && course.getOwnerId().equals(currentUser.getId())) {
			return;
		}

		throw new BusinessException(ErrorCode.COURSE_ACCESS_DENIED, "You do not have access to this course");
	}

	private Specification<Course> buildCourseSpecification(
		AuthenticatedUser currentUser,
		String keyword,
		CourseStatus status
	) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (!hasRole(currentUser, "ADMIN")) {
				if (hasRole(currentUser, "TEACHER")) {
					predicates.add(criteriaBuilder.or(
						criteriaBuilder.equal(root.get("ownerId"), currentUser.getId()),
						criteriaBuilder.equal(root.get("status"), CourseStatus.PUBLISHED)
					));
				}
				else {
					predicates.add(criteriaBuilder.equal(root.get("status"), CourseStatus.PUBLISHED));
				}
			}

			if (status != null) {
				predicates.add(criteriaBuilder.equal(root.get("status"), status));
			}

			if (StringUtils.hasText(keyword)) {
				String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
				predicates.add(criteriaBuilder.or(
					criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
					criteriaBuilder.like(criteriaBuilder.lower(root.get("subtitle")), pattern),
					criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern)
				));
			}

			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private boolean hasRole(AuthenticatedUser user, String role) {
		return hasAuthority(user, "ROLE_" + role);
	}

	private boolean hasAuthority(AuthenticatedUser user, String authority) {
		Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
		return authorities.stream().anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
	}

	private String normalizeOptionalText(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}

		return value.trim();
	}
}
