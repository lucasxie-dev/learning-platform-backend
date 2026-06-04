package dev.lucasxie.learning.auth.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

	private SecurityUtils() {
	}

	public static Optional<Long> getCurrentUserId() {
		return getCurrentUser().map(AuthenticatedUser::getId);
	}

	public static Optional<AuthenticatedUser> getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return Optional.empty();
		}

		Object principal = authentication.getPrincipal();
		if (principal instanceof AuthenticatedUser authenticatedUser) {
			return Optional.of(authenticatedUser);
		}

		return Optional.empty();
	}

	public static Optional<String> getCurrentUsername() {
		return getCurrentUser().map(AuthenticatedUser::getUsername);
	}
}
