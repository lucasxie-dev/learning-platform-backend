package dev.lucasxie.learning.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
	@NotBlank
	String refreshToken
) {
}
