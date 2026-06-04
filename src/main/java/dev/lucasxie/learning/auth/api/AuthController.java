package dev.lucasxie.learning.auth.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.lucasxie.learning.auth.dto.LoginRequest;
import dev.lucasxie.learning.auth.dto.RefreshTokenRequest;
import dev.lucasxie.learning.auth.dto.RegisterRequest;
import dev.lucasxie.learning.auth.dto.TokenResponse;
import dev.lucasxie.learning.auth.service.AuthService;
import dev.lucasxie.learning.common.api.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ApiResponse<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ApiResponse.success(authService.register(request));
	}

	@PostMapping("/login")
	public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.success(authService.login(request));
	}

	@PostMapping("/refresh")
	public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ApiResponse.success(authService.refresh(request));
	}
}
