package dev.lucasxie.learning.file.service;

import org.springframework.web.multipart.MultipartFile;

import dev.lucasxie.learning.common.api.PageResponse;
import dev.lucasxie.learning.file.FileAssetType;
import dev.lucasxie.learning.file.StorageProvider;
import dev.lucasxie.learning.file.dto.FileAccessUrlResponse;
import dev.lucasxie.learning.file.dto.FileAssetListItemResponse;
import dev.lucasxie.learning.file.dto.FileAssetResponse;
import dev.lucasxie.learning.file.dto.FileUploadResponse;

public interface FileAssetService {

	FileUploadResponse upload(MultipartFile file, FileAssetType assetType);

	FileAssetResponse getFile(Long fileId);

	PageResponse<FileAssetListItemResponse> listFiles(
		String keyword,
		FileAssetType assetType,
		StorageProvider storageProvider,
		String relatedType,
		Long relatedId,
		Boolean bound,
		int page,
		int size
	);

	FileDownload downloadFileContent(Long fileId);

	FileDownload downloadPublicFileContent(Long fileId, String token);

	FileAccessUrlResponse createAccessUrl(Long fileId);

	void deleteFile(Long fileId);

	FileAssetResponse bindCourseCover(Long courseId, Long fileId);

	FileAssetResponse bindLessonAudio(Long lessonId, Long fileId);

	FileAssetResponse bindLessonVideo(Long lessonId, Long fileId);

	FileAssetResponse bindLessonSubtitle(Long lessonId, Long fileId);

	void unbindCourseCover(Long courseId);

	void unbindLessonAudio(Long lessonId);

	void unbindLessonVideo(Long lessonId);

	void unbindLessonSubtitle(Long lessonId);
}
