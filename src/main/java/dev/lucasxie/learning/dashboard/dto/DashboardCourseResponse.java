package dev.lucasxie.learning.dashboard.dto;

import java.time.Instant;

import dev.lucasxie.learning.course.CourseStatus;

public record DashboardCourseResponse(
	Long id,
	String title,
	String subtitle,
	Long coverFileId,
	Long ownerId,
	CourseStatus status,
	Instant publishedAt,
	Instant createdAt,
	long lessonCount,
	long enrollmentCount
) {
}
