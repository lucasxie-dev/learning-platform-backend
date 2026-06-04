package dev.lucasxie.learning.progress.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LessonProgressUpdateRequest(
	@NotNull
	@Min(0)
	Integer progressSeconds
) {
}
