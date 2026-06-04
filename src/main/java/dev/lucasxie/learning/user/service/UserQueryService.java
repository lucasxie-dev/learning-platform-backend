package dev.lucasxie.learning.user.service;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.lucasxie.learning.auth.security.SecurityUtils;
import dev.lucasxie.learning.common.exception.BusinessException;
import dev.lucasxie.learning.common.exception.ErrorCode;
import dev.lucasxie.learning.permission.Permission;
import dev.lucasxie.learning.role.Role;
import dev.lucasxie.learning.user.UserAccount;
import dev.lucasxie.learning.user.UserAccountRepository;
import dev.lucasxie.learning.user.dto.CurrentUserResponse;

@Service
public class UserQueryService {

	private final UserAccountRepository userAccountRepository;

	public UserQueryService(UserAccountRepository userAccountRepository) {
		this.userAccountRepository = userAccountRepository;
	}

	@Transactional(readOnly = true)
	public CurrentUserResponse getCurrentUser() {
		Long userId = SecurityUtils.getCurrentUserId()
			.orElseThrow(() -> new BusinessException(ErrorCode.COMMON_UNAUTHORIZED, "Authentication is required"));

		UserAccount userAccount = userAccountRepository.findWithRolesById(userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND, "Current user was not found"));

		return toResponse(userAccount);
	}

	private CurrentUserResponse toResponse(UserAccount userAccount) {
		Set<String> roles = new LinkedHashSet<>();
		Set<String> permissions = new LinkedHashSet<>();

		for (Role role : userAccount.getRoles()) {
			roles.add(role.getCode().name());

			for (Permission permission : role.getPermissions()) {
				permissions.add(permission.getCode());
			}
		}

		return new CurrentUserResponse(
			userAccount.getId(),
			userAccount.getEmail(),
			userAccount.getUsername(),
			userAccount.getDisplayName(),
			userAccount.getAvatarUrl(),
			userAccount.getStatus(),
			roles,
			permissions
		);
	}
}
