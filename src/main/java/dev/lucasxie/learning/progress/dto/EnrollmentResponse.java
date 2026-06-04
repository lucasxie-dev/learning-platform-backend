package dev.lucasxie.learning.progress.dto;

import java.time.Instant;

import dev.lucasxie.learning.course.CourseStatus;

public record EnrollmentResponse(
	Long id,
	Long userId,
	Long courseId,
	Instant enrolledAt,
	String courseTitle,
	CourseStatus courseStatus
) {
}
