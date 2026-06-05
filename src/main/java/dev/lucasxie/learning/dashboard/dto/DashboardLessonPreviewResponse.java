package dev.lucasxie.learning.dashboard.dto;

public record DashboardLessonPreviewResponse(
	Long lessonId,
	Long courseId,
	String lessonTitle,
	String courseTitle,
	Long audioFileId,
	Long videoFileId,
	Long subtitleFileId,
	Integer durationSeconds
) {
}
