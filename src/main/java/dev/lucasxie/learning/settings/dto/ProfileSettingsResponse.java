package dev.lucasxie.learning.settings.dto;

import java.util.List;

public record ProfileSettingsResponse(
	Long id,
	String email,
	String username,
	String displayName,
	String avatarUrl,
	String status,
	List<String> roles,
	List<String> permissions
) {
}
