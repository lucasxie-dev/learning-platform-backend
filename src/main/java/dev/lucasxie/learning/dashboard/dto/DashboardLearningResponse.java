package dev.lucasxie.learning.dashboard.dto;

import java.time.Instant;

import dev.lucasxie.learning.course.CourseStatus;

public record DashboardLearningResponse(
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
