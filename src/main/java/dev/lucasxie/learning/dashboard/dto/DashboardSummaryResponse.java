package dev.lucasxie.learning.dashboard.dto;

public record DashboardSummaryResponse(
	long totalCourses,
	long publishedCourses,
	long draftCourses,
	long archivedCourses,
	long totalLessons,
	long totalEnrollments,
	long totalMediaAssets,
	double averageCompletionRate
) {
}
