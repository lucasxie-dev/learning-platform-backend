package dev.lucasxie.learning.lesson.mapper;

import org.springframework.stereotype.Component;

import dev.lucasxie.learning.lesson.Lesson;
import dev.lucasxie.learning.lesson.dto.GlobalLessonListItemResponse;
import dev.lucasxie.learning.lesson.dto.LessonListItemResponse;
import dev.lucasxie.learning.lesson.dto.LessonResponse;

@Component
public class LessonMapper {

	public LessonResponse toResponse(Lesson lesson) {
		return new LessonResponse(
			lesson.getId(),
			lesson.getCourseId(),
			lesson.getTitle(),
			lesson.getDescription(),
			lesson.getContentMarkdown(),
			lesson.getSortOrder(),
			lesson.getStatus(),
			lesson.getAudioFileId(),
			lesson.getVideoFileId(),
			lesson.getSubtitleFileId(),
			lesson.getDurationSeconds(),
			lesson.getCreatedAt(),
			lesson.getUpdatedAt()
		);
	}

	public LessonListItemResponse toListItemResponse(Lesson lesson) {
		return new LessonListItemResponse(
			lesson.getId(),
			lesson.getCourseId(),
			lesson.getTitle(),
			lesson.getSortOrder(),
			lesson.getStatus(),
			lesson.getDurationSeconds(),
			lesson.getCreatedAt(),
			lesson.getUpdatedAt()
		);
	}

	public GlobalLessonListItemResponse toGlobalListItemResponse(Lesson lesson, String courseTitle, Long ownerId) {
		return new GlobalLessonListItemResponse(
			lesson.getId(),
			lesson.getCourseId(),
			courseTitle,
			lesson.getTitle(),
			lesson.getDescription(),
			lesson.getSortOrder(),
			lesson.getStatus(),
			lesson.getAudioFileId(),
			lesson.getVideoFileId(),
			lesson.getSubtitleFileId(),
			lesson.getAudioFileId() != null,
			lesson.getVideoFileId() != null,
			lesson.getSubtitleFileId() != null,
			lesson.getDurationSeconds(),
			ownerId,
			lesson.getCreatedAt(),
			lesson.getUpdatedAt()
		);
	}
}
