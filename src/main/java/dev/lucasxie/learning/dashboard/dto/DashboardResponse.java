package dev.lucasxie.learning.dashboard.dto;

import java.util.List;

public record DashboardResponse(
	DashboardSummaryResponse summary,
	List<DashboardCourseResponse> recentCourses,
	List<DashboardLearningResponse> recentLearning,
	List<DashboardMediaResponse> recentMedia,
	DashboardLessonPreviewResponse lessonPreview,
	List<DashboardActivityResponse> recentActivities
) {
}
