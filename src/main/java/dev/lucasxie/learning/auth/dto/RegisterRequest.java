package dev.lucasxie.learning.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
	@NotBlank
	@Email
	@Size(max = 255)
	String email,

	@NotBlank
	@Size(max = 100)
	String username,

	@NotBlank
	@Size(min = 8, max = 100)
	String password,

	@Pattern(regexp = ".*\\S.*", message = "must not be blank")
	@Size(max = 100)
	String displayName
) {
}
