package dev.lucasxie.learning.file.api;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import dev.lucasxie.learning.common.api.ApiResponse;
import dev.lucasxie.learning.file.FileAssetType;
import dev.lucasxie.learning.file.dto.FileAccessUrlResponse;
import dev.lucasxie.learning.file.dto.FileAssetResponse;
import dev.lucasxie.learning.file.dto.FileUploadResponse;
import dev.lucasxie.learning.file.service.FileAssetService;
import dev.lucasxie.learning.file.service.FileDownload;

@RestController
@RequestMapping("/api/v1/files")
public class FileAssetController {

	private final FileAssetService fileAssetService;

	public FileAssetController(FileAssetService fileAssetService) {
		this.fileAssetService = fileAssetService;
	}

	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasAuthority('file:upload')")
	public ApiResponse<FileUploadResponse> upload(
		@RequestParam MultipartFile file,
		@RequestParam FileAssetType assetType
	) {
		return ApiResponse.success(fileAssetService.upload(file, assetType));
	}

	@GetMapping("/{fileId}")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<FileAssetResponse> getFile(@PathVariable Long fileId) {
		return ApiResponse.success(fileAssetService.getFile(fileId));
	}

	@PostMapping("/{fileId}/access-url")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<FileAccessUrlResponse> createAccessUrl(@PathVariable Long fileId) {
		return ApiResponse.success(fileAssetService.createAccessUrl(fileId));
	}

	@GetMapping("/{fileId}/content")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Resource> downloadFileContent(@PathVariable Long fileId) {
		return toContentResponse(fileAssetService.downloadFileContent(fileId));
	}

	@GetMapping("/{fileId}/public-content")
	public ResponseEntity<Resource> downloadPublicFileContent(
		@PathVariable Long fileId,
		@RequestParam String token
	) {
		return toContentResponse(fileAssetService.downloadPublicFileContent(fileId, token));
	}

	private ResponseEntity<Resource> toContentResponse(FileDownload download) {
		ByteArrayResource resource = new ByteArrayResource(download.content());
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(download.contentType()))
			.contentLength(download.sizeBytes())
			.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
				.filename(download.originalName())
				.build()
				.toString())
			.body(resource);
	}

	@DeleteMapping("/{fileId}")
	@PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasAuthority('file:delete')")
	public ApiResponse<Void> deleteFile(@PathVariable Long fileId) {
		fileAssetService.deleteFile(fileId);
		return ApiResponse.success();
	}
}
