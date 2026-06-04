package dev.lucasxie.learning.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CourseUpdateRequest(
	@NotBlank
	@Size(max = 200)
	String title,

	@Pattern(regexp = ".*\\S.*", message = "must not be blank")
	@Size(max = 300)
	String subtitle,

	@Pattern(regexp = ".*\\S.*", message = "must not be blank")
	@Size(max = 10000)
	String description
) {
}
