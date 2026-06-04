package dev.lucasxie.learning.lesson.service;

import java.util.List;

import dev.lucasxie.learning.lesson.dto.LessonCreateRequest;
import dev.lucasxie.learning.lesson.dto.LessonListItemResponse;
import dev.lucasxie.learning.lesson.dto.LessonReorderRequest;
import dev.lucasxie.learning.lesson.dto.LessonResponse;
import dev.lucasxie.learning.lesson.dto.LessonUpdateRequest;

public interface LessonService {

	LessonResponse createLesson(Long courseId, LessonCreateRequest request);

	LessonResponse updateLesson(Long lessonId, LessonUpdateRequest request);

	LessonResponse getLesson(Long lessonId);

	List<LessonListItemResponse> listLessonsByCourse(Long courseId);

	LessonResponse publishLesson(Long lessonId);

	LessonResponse archiveLesson(Long lessonId);

	void deleteLesson(Long lessonId);

	void reorderLessons(Long courseId, LessonReorderRequest request);
}
