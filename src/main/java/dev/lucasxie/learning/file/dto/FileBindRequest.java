package dev.lucasxie.learning.file.dto;

import jakarta.validation.constraints.NotNull;

public record FileBindRequest(
	@NotNull Long fileId
) {
}
