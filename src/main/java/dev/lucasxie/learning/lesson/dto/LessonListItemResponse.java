package dev.lucasxie.learning.lesson.dto;

import java.time.Instant;

import dev.lucasxie.learning.lesson.LessonStatus;

public record LessonListItemResponse(
	Long id,
	Long courseId,
	String title,
	Integer sortOrder,
	LessonStatus status,
	Integer durationSeconds,
	Instant createdAt,
	Instant updatedAt
) {
}
