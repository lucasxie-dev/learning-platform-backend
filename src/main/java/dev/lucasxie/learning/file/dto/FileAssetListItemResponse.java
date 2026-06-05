package dev.lucasxie.learning.file.dto;

import java.time.Instant;

import dev.lucasxie.learning.file.FileAssetType;
import dev.lucasxie.learning.file.StorageProvider;

public record FileAssetListItemResponse(
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
	Boolean bound,
	String checksum,
	Instant createdAt,
	Instant updatedAt
) {
}
