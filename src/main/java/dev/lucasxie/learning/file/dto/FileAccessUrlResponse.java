package dev.lucasxie.learning.file.dto;

import java.time.Instant;

public record FileAccessUrlResponse(
	String url,
	Instant expiresAt
) {
}
