package dev.lucasxie.learning.file.dto;

import java.time.Instant;

import dev.lucasxie.learning.file.FileAssetType;
import dev.lucasxie.learning.file.StorageProvider;

public record FileAssetResponse(
	Long id,
	String originalName,
	String url,
	String contentType,
	Long sizeBytes,
	FileAssetType assetType,
	StorageProvider storageProvider,
	Long ownerId,
	String relatedType,
	Long relatedId,
	String checksum,
	Instant createdAt,
	Instant updatedAt
) {
}
