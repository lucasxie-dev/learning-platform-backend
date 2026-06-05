package dev.lucasxie.learning.dashboard.dto;

import java.time.Instant;

import dev.lucasxie.learning.file.FileAssetType;
import dev.lucasxie.learning.file.StorageProvider;

public record DashboardMediaResponse(
	Long id,
	String originalName,
	String contentType,
	long sizeBytes,
	FileAssetType assetType,
	StorageProvider storageProvider,
	Instant createdAt
) {
}
