package dev.lucasxie.learning.lesson.service;

import java.util.List;

import dev.lucasxie.learning.common.api.PageResponse;
import dev.lucasxie.learning.lesson.LessonStatus;
import dev.lucasxie.learning.lesson.dto.GlobalLessonListItemResponse;
import dev.lucasxie.learning.lesson.dto.LessonCreateRequest;
import dev.lucasxie.learning.lesson.dto.LessonListItemResponse;
import dev.lucasxie.learning.lesson.dto.LessonReorderRequest;
import dev.lucasxie.learning.lesson.dto.LessonResponse;
import dev.lucasxie.learning.lesson.dto.LessonUpdateRequest;

public interface LessonService {

	LessonResponse createLesson(Long courseId, LessonCreateRequest request);

	LessonResponse updateLesson(Long lessonId, LessonUpdateRequest request);

	LessonResponse getLesson(Long lessonId);

	PageResponse<GlobalLessonListItemResponse> listLessons(
		String keyword,
		Long courseId,
		LessonStatus status,
		Boolean hasAudio,
		Boolean hasVideo,
		Boolean hasSubtitle,
		int page,
		int size
	);

	List<LessonListItemResponse> listLessonsByCourse(Long courseId);

	LessonResponse publishLesson(Long lessonId);

	LessonResponse archiveLesson(Long lessonId);

	void deleteLesson(Long lessonId);

	void reorderLessons(Long courseId, LessonReorderRequest request);
}
