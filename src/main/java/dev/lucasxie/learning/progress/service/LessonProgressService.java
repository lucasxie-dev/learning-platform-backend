package dev.lucasxie.learning.progress.service;

import dev.lucasxie.learning.progress.dto.CourseProgressResponse;
import dev.lucasxie.learning.progress.dto.LessonProgressResponse;
import dev.lucasxie.learning.progress.dto.LessonProgressUpdateRequest;

public interface LessonProgressService {

	LessonProgressResponse updateLessonProgress(Long lessonId, LessonProgressUpdateRequest request);

	LessonProgressResponse completeLesson(Long lessonId);

	CourseProgressResponse getMyCourseProgress(Long courseId);

	CourseProgressResponse getUserCourseProgress(Long courseId, Long userId);
}
