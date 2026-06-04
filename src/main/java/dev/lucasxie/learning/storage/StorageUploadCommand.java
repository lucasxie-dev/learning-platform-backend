package dev.lucasxie.learning.storage;

import java.io.InputStream;

import dev.lucasxie.learning.file.FileAssetType;

public record StorageUploadCommand(
	String originalName,
	String contentType,
	long sizeBytes,
	InputStream inputStream,
	FileAssetType assetType,
	Long ownerId,
	Long fileAssetId
) {
}
