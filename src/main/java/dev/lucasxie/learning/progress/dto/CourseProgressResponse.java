package dev.lucasxie.learning.progress.dto;

import java.util.List;

public record CourseProgressResponse(
	Long courseId,
	Long userId,
	long totalLessons,
	long completedLessons,
	double completionRate,
	List<LessonProgressItemResponse> lessons
) {
}
