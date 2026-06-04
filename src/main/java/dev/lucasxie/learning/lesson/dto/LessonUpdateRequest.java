package dev.lucasxie.learning.lesson.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LessonUpdateRequest(
	@NotBlank
	@Size(max = 200)
	String title,

	@Pattern(regexp = ".*\\S.*", message = "must not be blank")
	@Size(max = 10000)
	String description,

	@Min(0)
	Integer sortOrder
) {
}
