package dev.lucasxie.learning.course.service;

import dev.lucasxie.learning.common.api.PageResponse;
import dev.lucasxie.learning.course.CourseStatus;
import dev.lucasxie.learning.course.dto.CourseCreateRequest;
import dev.lucasxie.learning.course.dto.CourseListItemResponse;
import dev.lucasxie.learning.course.dto.CourseResponse;
import dev.lucasxie.learning.course.dto.CourseUpdateRequest;

public interface CourseService {

	CourseResponse createCourse(CourseCreateRequest request);

	CourseResponse updateCourse(Long courseId, CourseUpdateRequest request);

	CourseResponse getCourse(Long courseId);

	PageResponse<CourseListItemResponse> listCourses(String keyword, CourseStatus status, int page, int size);

	CourseResponse publishCourse(Long courseId);

	CourseResponse archiveCourse(Long courseId);

	void deleteCourse(Long courseId);
}
