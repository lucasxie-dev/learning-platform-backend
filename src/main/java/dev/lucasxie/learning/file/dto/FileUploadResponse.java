package dev.lucasxie.learning.file.dto;

import java.time.Instant;

import dev.lucasxie.learning.file.FileAssetType;
import dev.lucasxie.learning.file.StorageProvider;

public record FileUploadResponse(
	Long id,
	String originalName,
	String url,
	String contentType,
	Long sizeBytes,
	FileAssetType assetType,
	StorageProvider storageProvider,
	String checksum,
	Instant createdAt
) {
}
