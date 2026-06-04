package dev.lucasxie.learning.course.dto;

import java.time.Instant;

import dev.lucasxie.learning.course.CourseStatus;

public record CourseListItemResponse(
	Long id,
	String title,
	String subtitle,
	Long coverFileId,
	Long ownerId,
	CourseStatus status,
	Instant publishedAt,
	Instant createdAt
) {
}
