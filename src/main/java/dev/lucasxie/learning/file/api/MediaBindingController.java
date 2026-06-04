package dev.lucasxie.learning.file.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.lucasxie.learning.common.api.ApiResponse;
import dev.lucasxie.learning.file.dto.FileAssetResponse;
import dev.lucasxie.learning.file.dto.FileBindRequest;
import dev.lucasxie.learning.file.service.FileAssetService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class MediaBindingController {

	private final FileAssetService fileAssetService;

	public MediaBindingController(FileAssetService fileAssetService) {
		this.fileAssetService = fileAssetService;
	}

	@PutMapping("/courses/{courseId}/cover")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasAuthority('course:update')")
	public ApiResponse<FileAssetResponse> bindCourseCover(
		@PathVariable Long courseId,
		@Valid @RequestBody FileBindRequest request
	) {
		return ApiResponse.success(fileAssetService.bindCourseCover(courseId, request.fileId()));
	}

	@DeleteMapping("/courses/{courseId}/cover")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasAuthority('course:update')")
	public ApiResponse<Void> unbindCourseCover(@PathVariable Long courseId) {
		fileAssetService.unbindCourseCover(courseId);
		return ApiResponse.success();
	}

	@PutMapping("/lessons/{lessonId}/audio")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasAuthority('course:update')")
	public ApiResponse<FileAssetResponse> bindLessonAudio(
		@PathVariable Long lessonId,
		@Valid @RequestBody FileBindRequest request
	) {
		return ApiResponse.success(fileAssetService.bindLessonAudio(lessonId, request.fileId()));
	}

	@DeleteMapping("/lessons/{lessonId}/audio")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasAuthority('course:update')")
	public ApiResponse<Void> unbindLessonAudio(@PathVariable Long lessonId) {
		fileAssetService.unbindLessonAudio(lessonId);
		return ApiResponse.success();
	}

	@PutMapping("/lessons/{lessonId}/video")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasAuthority('course:update')")
	public ApiResponse<FileAssetResponse> bindLessonVideo(
		@PathVariable Long lessonId,
		@Valid @RequestBody FileBindRequest request
	) {
		return ApiResponse.success(fileAssetService.bindLessonVideo(lessonId, request.fileId()));
	}

	@DeleteMapping("/lessons/{lessonId}/video")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasAuthority('course:update')")
	public ApiResponse<Void> unbindLessonVideo(@PathVariable Long lessonId) {
		fileAssetService.unbindLessonVideo(lessonId);
		return ApiResponse.success();
	}

	@PutMapping("/lessons/{lessonId}/subtitle")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasAuthority('course:update')")
	public ApiResponse<FileAssetResponse> bindLessonSubtitle(
		@PathVariable Long lessonId,
		@Valid @RequestBody FileBindRequest request
	) {
		return ApiResponse.success(fileAssetService.bindLessonSubtitle(lessonId, request.fileId()));
	}

	@DeleteMapping("/lessons/{lessonId}/subtitle")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasAuthority('course:update')")
	public ApiResponse<Void> unbindLessonSubtitle(@PathVariable Long lessonId) {
		fileAssetService.unbindLessonSubtitle(lessonId);
		return ApiResponse.success();
	}
}
