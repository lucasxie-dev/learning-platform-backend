package dev.lucasxie.learning.lesson.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record LessonReorderRequest(
	@NotEmpty
	List<@Valid LessonOrderItem> items
) {
}
