package dev.lucasxie.learning.storage;

public record StorageDownloadResult(
	byte[] content,
	String contentType,
	long sizeBytes,
	String originalName
) {
}
