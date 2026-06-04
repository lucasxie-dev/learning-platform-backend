package dev.lucasxie.learning.progress.service;

import dev.lucasxie.learning.common.api.PageResponse;
import dev.lucasxie.learning.progress.dto.EnrollmentResponse;
import dev.lucasxie.learning.progress.dto.MyCourseResponse;

public interface EnrollmentService {

	EnrollmentResponse enrollCourse(Long courseId);

	PageResponse<MyCourseResponse> listMyCourses(int page, int size);

	PageResponse<EnrollmentResponse> listCourseEnrollments(Long courseId, int page, int size);
}
