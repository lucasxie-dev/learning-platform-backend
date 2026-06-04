package dev.lucasxie.learning.progress.dto;

import java.time.Instant;

import dev.lucasxie.learning.course.CourseStatus;

public record MyCourseResponse(
	Long enrollmentId,
	Long courseId,
	String title,
	String subtitle,
	Long coverFileId,
	CourseStatus status,
	Instant enrolledAt,
	long totalLessons,
	long completedLessons,
	double completionRate,
	Instant lastStudiedAt
) {
}
