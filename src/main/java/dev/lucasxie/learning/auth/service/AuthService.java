package dev.lucasxie.learning.auth.service;

import java.time.Instant;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import dev.lucasxie.learning.auth.dto.LoginRequest;
import dev.lucasxie.learning.auth.dto.RefreshTokenRequest;
import dev.lucasxie.learning.auth.dto.RegisterRequest;
import dev.lucasxie.learning.auth.dto.TokenResponse;
import dev.lucasxie.learning.auth.security.AuthenticatedUser;
import dev.lucasxie.learning.auth.security.CustomUserDetailsService;
import dev.lucasxie.learning.auth.security.JwtTokenProvider;
import dev.lucasxie.learning.common.exception.BusinessException;
import dev.lucasxie.learning.common.exception.ErrorCode;
import dev.lucasxie.learning.role.Role;
import dev.lucasxie.learning.role.RoleCode;
import dev.lucasxie.learning.role.RoleRepository;
import dev.lucasxie.learning.user.UserAccount;
import dev.lucasxie.learning.user.UserAccountRepository;
import dev.lucasxie.learning.user.UserStatus;

@Service
public class AuthService {

	private static final String TOKEN_TYPE = "Bearer";

	private final UserAccountRepository userAccountRepository;

	private final RoleRepository roleRepository;

	private final PasswordEncoder passwordEncoder;

	private final JwtTokenProvider jwtTokenProvider;

	private final CustomUserDetailsService customUserDetailsService;

	public AuthService(
		UserAccountRepository userAccountRepository,
		RoleRepository roleRepository,
		PasswordEncoder passwordEncoder,
		JwtTokenProvider jwtTokenProvider,
		CustomUserDetailsService customUserDetailsService
	) {
		this.userAccountRepository = userAccountRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
		this.customUserDetailsService = customUserDetailsService;
	}

	@Transactional
	public TokenResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		String username = request.username().trim();

		if (userAccountRepository.existsByEmailIgnoreCase(email)) {
			throw new BusinessException(ErrorCode.COMMON_CONFLICT, "Email is already registered");
		}

		if (userAccountRepository.existsByUsernameIgnoreCase(username)) {
			throw new BusinessException(ErrorCode.COMMON_CONFLICT, "Username is already registered");
		}

		Role studentRole = roleRepository.findByCode(RoleCode.STUDENT)
			.orElseThrow(() -> new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "Default student role is missing"));

		UserAccount userAccount = new UserAccount();
		userAccount.setEmail(email);
		userAccount.setUsername(username);
		userAccount.setPasswordHash(passwordEncoder.encode(request.password()));
		userAccount.setDisplayName(normalizeOptionalText(request.displayName()));
		userAccount.setStatus(UserStatus.ACTIVE);
		userAccount.getRoles().add(studentRole);

		UserAccount savedUserAccount = userAccountRepository.save(userAccount);
		AuthenticatedUser authenticatedUser = customUserDetailsService.toAuthenticatedUser(savedUserAccount);

		return createTokenResponse(authenticatedUser);
	}

	@Transactional
	public TokenResponse login(LoginRequest request) {
		String account = request.account().trim();
		UserAccount userAccount = userAccountRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(account, account)
			.orElseThrow(() -> new BusinessException(ErrorCode.COMMON_UNAUTHORIZED, "Invalid account or password"));

		if (userAccount.getStatus() == UserStatus.DISABLED) {
			throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "User account is disabled");
		}

		if (userAccount.getStatus() == UserStatus.LOCKED) {
			throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "User account is locked");
		}

		if (!passwordEncoder.matches(request.password(), userAccount.getPasswordHash())) {
			throw new BusinessException(ErrorCode.COMMON_UNAUTHORIZED, "Invalid account or password");
		}

		userAccount.setLastLoginAt(Instant.now());
		AuthenticatedUser authenticatedUser = customUserDetailsService.toAuthenticatedUser(userAccount);

		return createTokenResponse(authenticatedUser);
	}

	@Transactional(readOnly = true)
	public TokenResponse refresh(RefreshTokenRequest request) {
		String refreshToken = request.refreshToken().trim();
		if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
			throw new BusinessException(ErrorCode.COMMON_UNAUTHORIZED, "Invalid refresh token");
		}

		Long userId = jwtTokenProvider.parseUserId(refreshToken);
		AuthenticatedUser authenticatedUser = customUserDetailsService.loadUserById(userId);
		if (!authenticatedUser.isEnabled() || !authenticatedUser.isAccountNonLocked()) {
			throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "User account is not active");
		}

		String accessToken = jwtTokenProvider.generateAccessToken(authenticatedUser);
		return new TokenResponse(
			accessToken,
			refreshToken,
			TOKEN_TYPE,
			jwtTokenProvider.getAccessTokenExpiresInSeconds()
		);
	}

	private TokenResponse createTokenResponse(AuthenticatedUser authenticatedUser) {
		return new TokenResponse(
			jwtTokenProvider.generateAccessToken(authenticatedUser),
			jwtTokenProvider.generateRefreshToken(authenticatedUser),
			TOKEN_TYPE,
			jwtTokenProvider.getAccessTokenExpiresInSeconds()
		);
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private String normalizeOptionalText(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}

		return value.trim();
	}
}
