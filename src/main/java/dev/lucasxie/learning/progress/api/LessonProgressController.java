package dev.lucasxie.learning.progress.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.lucasxie.learning.common.api.ApiResponse;
import dev.lucasxie.learning.progress.dto.CourseProgressResponse;
import dev.lucasxie.learning.progress.dto.LessonProgressResponse;
import dev.lucasxie.learning.progress.dto.LessonProgressUpdateRequest;
import dev.lucasxie.learning.progress.service.LessonProgressService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class LessonProgressController {

	private final LessonProgressService lessonProgressService;

	public LessonProgressController(LessonProgressService lessonProgressService) {
		this.lessonProgressService = lessonProgressService;
	}

	@PutMapping("/lessons/{lessonId}/progress")
	@PreAuthorize("hasRole('STUDENT')")
	public ApiResponse<LessonProgressResponse> updateLessonProgress(
		@PathVariable Long lessonId,
		@Valid @RequestBody LessonProgressUpdateRequest request
	) {
		return ApiResponse.success(lessonProgressService.updateLessonProgress(lessonId, request));
	}

	@PostMapping("/lessons/{lessonId}/complete")
	@PreAuthorize("hasRole('STUDENT')")
	public ApiResponse<LessonProgressResponse> completeLesson(@PathVariable Long lessonId) {
		return ApiResponse.success(lessonProgressService.completeLesson(lessonId));
	}

	@GetMapping("/courses/{courseId}/progress")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<CourseProgressResponse> getMyCourseProgress(@PathVariable Long courseId) {
		return ApiResponse.success(lessonProgressService.getMyCourseProgress(courseId));
	}

	@GetMapping("/courses/{courseId}/users/{userId}/progress")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
	public ApiResponse<CourseProgressResponse> getUserCourseProgress(
		@PathVariable Long courseId,
		@PathVariable Long userId
	) {
		return ApiResponse.success(lessonProgressService.getUserCourseProgress(courseId, userId));
	}
}
