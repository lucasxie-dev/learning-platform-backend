package dev.lucasxie.learning.file.service;

public record FileDownload(
	byte[] content,
	String contentType,
	long sizeBytes,
	String originalName
) {
}
