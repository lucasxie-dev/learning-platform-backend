package dev.lucasxie.learning.lesson.api;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.lucasxie.learning.common.api.ApiResponse;
import dev.lucasxie.learning.common.api.PageResponse;
import dev.lucasxie.learning.lesson.LessonStatus;
import dev.lucasxie.learning.lesson.dto.GlobalLessonListItemResponse;
import dev.lucasxie.learning.lesson.dto.LessonCreateRequest;
import dev.lucasxie.learning.lesson.dto.LessonListItemResponse;
import dev.lucasxie.learning.lesson.dto.LessonReorderRequest;
import dev.lucasxie.learning.lesson.dto.LessonResponse;
import dev.lucasxie.learning.lesson.dto.LessonUpdateRequest;
import dev.lucasxie.learning.lesson.service.LessonService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class LessonController {

	private static final int MAX_PAGE_SIZE = 100;

	private final LessonService lessonService;

	public LessonController(LessonService lessonService) {
		this.lessonService = lessonService;
	}

	@PostMapping("/courses/{courseId}/lessons")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasAuthority('course:update')")
	public ApiResponse<LessonResponse> createLesson(
		@PathVariable Long courseId,
		@Valid @RequestBody LessonCreateRequest request
	) {
		return ApiResponse.success(lessonService.createLesson(courseId, request));
	}

	@GetMapping("/courses/{courseId}/lessons")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<List<LessonListItemResponse>> listLessonsByCourse(@PathVariable Long courseId) {
		return ApiResponse.success(lessonService.listLessonsByCourse(courseId));
	}

	@GetMapping("/lessons")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
	public ApiResponse<PageResponse<GlobalLessonListItemResponse>> listLessons(
		@RequestParam(required = false) String keyword,
		@RequestParam(required = false) Long courseId,
		@RequestParam(required = false) LessonStatus status,
		@RequestParam(required = false) Boolean hasAudio,
		@RequestParam(required = false) Boolean hasVideo,
		@RequestParam(required = false) Boolean hasSubtitle,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResponse.success(lessonService.listLessons(
			keyword,
			courseId,
			status,
			hasAudio,
			hasVideo,
			hasSubtitle,
			Math.max(page, 0),
			normalizeSize(size)
		));
	}

	@GetMapping("/lessons/{lessonId}")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<LessonResponse> getLesson(@PathVariable Long lessonId) {
		return ApiResponse.success(lessonService.getLesson(lessonId));
	}

	@PutMapping("/lessons/{lessonId}")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasAuthority('course:update')")
	public ApiResponse<LessonResponse> updateLesson(
		@PathVariable Long lessonId,
		@Valid @RequestBody LessonUpdateRequest request
	) {
		return ApiResponse.success(lessonService.updateLesson(lessonId, request));
	}

	@PostMapping("/lessons/{lessonId}/publish")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasAuthority('course:publish')")
	public ApiResponse<LessonResponse> publishLesson(@PathVariable Long lessonId) {
		return ApiResponse.success(lessonService.publishLesson(lessonId));
	}

	@PostMapping("/lessons/{lessonId}/archive")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasAuthority('course:update')")
	public ApiResponse<LessonResponse> archiveLesson(@PathVariable Long lessonId) {
		return ApiResponse.success(lessonService.archiveLesson(lessonId));
	}

	@PatchMapping("/courses/{courseId}/lessons/reorder")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasAuthority('course:update')")
	public ApiResponse<Void> reorderLessons(
		@PathVariable Long courseId,
		@Valid @RequestBody LessonReorderRequest request
	) {
		lessonService.reorderLessons(courseId, request);
		return ApiResponse.success();
	}

	@DeleteMapping("/lessons/{lessonId}")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasAuthority('course:delete')")
	public ApiResponse<Void> deleteLesson(@PathVariable Long lessonId) {
		lessonService.deleteLesson(lessonId);
		return ApiResponse.success();
	}

	private int normalizeSize(int size) {
		return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
	}
}
