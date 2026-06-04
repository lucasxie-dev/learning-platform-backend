package dev.lucasxie.learning.progress.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.lucasxie.learning.common.api.ApiResponse;
import dev.lucasxie.learning.common.api.PageResponse;
import dev.lucasxie.learning.progress.dto.EnrollmentResponse;
import dev.lucasxie.learning.progress.dto.MyCourseResponse;
import dev.lucasxie.learning.progress.service.EnrollmentService;

@RestController
@RequestMapping("/api/v1")
public class EnrollmentController {

	private static final int MAX_PAGE_SIZE = 100;

	private final EnrollmentService enrollmentService;

	public EnrollmentController(EnrollmentService enrollmentService) {
		this.enrollmentService = enrollmentService;
	}

	@PostMapping("/courses/{courseId}/enroll")
	@PreAuthorize("hasRole('STUDENT')")
	public ApiResponse<EnrollmentResponse> enrollCourse(@PathVariable Long courseId) {
		return ApiResponse.success(enrollmentService.enrollCourse(courseId));
	}

	@GetMapping("/me/courses")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<PageResponse<MyCourseResponse>> listMyCourses(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResponse.success(enrollmentService.listMyCourses(Math.max(page, 0), normalizeSize(size)));
	}

	@GetMapping("/courses/{courseId}/enrollments")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
	public ApiResponse<PageResponse<EnrollmentResponse>> listCourseEnrollments(
		@PathVariable Long courseId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResponse.success(enrollmentService.listCourseEnrollments(courseId, Math.max(page, 0), normalizeSize(size)));
	}

	private int normalizeSize(int size) {
		return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
	}
}
