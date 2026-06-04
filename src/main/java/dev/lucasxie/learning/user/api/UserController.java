package dev.lucasxie.learning.user.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.lucasxie.learning.common.api.ApiResponse;
import dev.lucasxie.learning.user.dto.CurrentUserResponse;
import dev.lucasxie.learning.user.service.UserQueryService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserQueryService userQueryService;

	public UserController(UserQueryService userQueryService) {
		this.userQueryService = userQueryService;
	}

	@GetMapping("/me")
	public ApiResponse<CurrentUserResponse> getCurrentUser() {
		return ApiResponse.success(userQueryService.getCurrentUser());
	}

	@GetMapping("/admin-check")
	@PreAuthorize("hasRole('ADMIN') or hasAuthority('user:manage')")
	public ApiResponse<String> adminCheck() {
		return ApiResponse.success("ok");
	}
}
