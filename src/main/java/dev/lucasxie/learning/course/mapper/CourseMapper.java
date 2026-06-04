package dev.lucasxie.learning.course.mapper;

import org.springframework.stereotype.Component;

import dev.lucasxie.learning.course.Course;
import dev.lucasxie.learning.course.dto.CourseListItemResponse;
import dev.lucasxie.learning.course.dto.CourseResponse;

@Component
public class CourseMapper {

	public CourseResponse toResponse(Course course) {
		return new CourseResponse(
			course.getId(),
			course.getTitle(),
			course.getSubtitle(),
			course.getDescription(),
			course.getCoverFileId(),
			course.getOwnerId(),
			course.getStatus(),
			course.getPublishedAt(),
			course.getCreatedAt(),
			course.getUpdatedAt()
		);
	}

	public CourseListItemResponse toListItemResponse(Course course) {
		return new CourseListItemResponse(
			course.getId(),
			course.getTitle(),
			course.getSubtitle(),
			course.getCoverFileId(),
			course.getOwnerId(),
			course.getStatus(),
			course.getPublishedAt(),
			course.getCreatedAt()
		);
	}
}
