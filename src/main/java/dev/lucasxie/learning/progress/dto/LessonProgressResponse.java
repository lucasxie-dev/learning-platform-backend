package dev.lucasxie.learning.progress.dto;

import java.time.Instant;

public record LessonProgressResponse(
	Long id,
	Long userId,
	Long courseId,
	Long lessonId,
	Integer progressSeconds,
	Boolean completed,
	Instant completedAt,
	Instant createdAt,
	Instant updatedAt
) {
}
