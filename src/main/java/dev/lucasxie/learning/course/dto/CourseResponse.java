package dev.lucasxie.learning.course.dto;

import java.time.Instant;

import dev.lucasxie.learning.course.CourseStatus;

public record CourseResponse(
	Long id,
	String title,
	String subtitle,
	String description,
	Long coverFileId,
	Long ownerId,
	CourseStatus status,
	Instant publishedAt,
	Instant createdAt,
	Instant updatedAt
) {
}
