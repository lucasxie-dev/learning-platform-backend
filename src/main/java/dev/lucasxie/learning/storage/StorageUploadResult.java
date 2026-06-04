package dev.lucasxie.learning.storage;

import dev.lucasxie.learning.file.StorageProvider;

public record StorageUploadResult(
	String storageKey,
	String url,
	String contentType,
	long sizeBytes,
	String checksum,
	StorageProvider storageProvider
) {
}
