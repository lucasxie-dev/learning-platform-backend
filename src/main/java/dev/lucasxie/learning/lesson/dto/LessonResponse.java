package dev.lucasxie.learning.lesson.dto;

import java.time.Instant;

import dev.lucasxie.learning.lesson.LessonStatus;

public record LessonResponse(
	Long id,
	Long courseId,
	String title,
	String description,
	String contentMarkdown,
	Integer sortOrder,
	LessonStatus status,
	Long audioFileId,
	Long videoFileId,
	Long subtitleFileId,
	Integer durationSeconds,
	Instant createdAt,
	Instant updatedAt
) {
}
