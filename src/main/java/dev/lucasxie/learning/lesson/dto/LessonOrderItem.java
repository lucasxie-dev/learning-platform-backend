package dev.lucasxie.learning.lesson.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LessonOrderItem(
	@NotNull
	Long lessonId,

	@NotNull
	@Min(0)
	Integer sortOrder
) {
}
