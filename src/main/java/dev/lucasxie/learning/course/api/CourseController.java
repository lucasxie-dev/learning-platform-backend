package dev.lucasxie.learning.course.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.lucasxie.learning.common.api.ApiResponse;
import dev.lucasxie.learning.common.api.PageResponse;
import dev.lucasxie.learning.course.CourseStatus;
import dev.lucasxie.learning.course.dto.CourseCreateRequest;
import dev.lucasxie.learning.course.dto.CourseListItemResponse;
import dev.lucasxie.learning.course.dto.CourseResponse;
import dev.lucasxie.learning.course.dto.CourseUpdateRequest;
import dev.lucasxie.learning.course.service.CourseService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

	private static final int MAX_PAGE_SIZE = 100;

	private final CourseService courseService;

	public CourseController(CourseService courseService) {
		this.courseService = courseService;
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasAuthority('course:create')")
	public ApiResponse<CourseResponse> createCourse(@Valid @RequestBody CourseCreateRequest request) {
		return ApiResponse.success(courseService.createCourse(request));
	}

	@PutMapping("/{courseId}")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasAuthority('course:update')")
	public ApiResponse<CourseResponse> updateCourse(
		@PathVariable Long courseId,
		@Valid @RequestBody CourseUpdateRequest request
	) {
		return ApiResponse.success(courseService.updateCourse(courseId, request));
	}

	@GetMapping
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<PageResponse<CourseListItemResponse>> listCourses(
		@RequestParam(required = false) String keyword,
		@RequestParam(required = false) CourseStatus status,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResponse.success(courseService.listCourses(keyword, status, Math.max(page, 0), normalizeSize(size)));
	}

	@GetMapping("/{courseId}")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<CourseResponse> getCourse(@PathVariable Long courseId) {
		return ApiResponse.success(courseService.getCourse(courseId));
	}

	@PostMapping("/{courseId}/publish")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasAuthority('course:publish')")
	public ApiResponse<CourseResponse> publishCourse(@PathVariable Long courseId) {
		return ApiResponse.success(courseService.publishCourse(courseId));
	}

	@PostMapping("/{courseId}/archive")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasAuthority('course:update')")
	public ApiResponse<CourseResponse> archiveCourse(@PathVariable Long courseId) {
		return ApiResponse.success(courseService.archiveCourse(courseId));
	}

	@DeleteMapping("/{courseId}")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasAuthority('course:delete')")
	public ApiResponse<Void> deleteCourse(@PathVariable Long courseId) {
		courseService.deleteCourse(courseId);
		return ApiResponse.success();
	}

	private int normalizeSize(int size) {
		return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
	}
}
