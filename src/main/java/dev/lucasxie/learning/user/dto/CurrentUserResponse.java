package dev.lucasxie.learning.user.dto;

import java.util.Set;

import dev.lucasxie.learning.user.UserStatus;

public record CurrentUserResponse(
	Long id,
	String email,
	String username,
	String displayName,
	String avatarUrl,
	UserStatus status,
	Set<String> roles,
	Set<String> permissions
) {
}
