package dev.lucasxie.learning.lesson.dto;

import java.time.Instant;

import dev.lucasxie.learning.lesson.LessonStatus;

public record GlobalLessonListItemResponse(
	Long id,
	Long courseId,
	String courseTitle,
	String title,
	String description,
	Integer sortOrder,
	LessonStatus status,
	Long audioFileId,
	Long videoFileId,
	Long subtitleFileId,
	Boolean hasAudio,
	Boolean hasVideo,
	Boolean hasSubtitle,
	Integer durationSeconds,
	Long ownerId,
	Instant createdAt,
	Instant updatedAt
) {
}
