package dev.lucasxie.learning.progress.dto;

import java.time.Instant;

public record LessonProgressItemResponse(
	Long lessonId,
	String title,
	Integer sortOrder,
	Integer durationSeconds,
	Integer progressSeconds,
	Boolean completed,
	Instant completedAt,
	Instant updatedAt
) {
}
