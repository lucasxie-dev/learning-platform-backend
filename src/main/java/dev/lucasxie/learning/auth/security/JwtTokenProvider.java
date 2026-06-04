package dev.lucasxie.learning.auth.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;

@Component
public class JwtTokenProvider {

	private static final String AUTHORITIES_CLAIM = "authorities";

	private static final String TOKEN_TYPE_CLAIM = "typ";

	private static final String ACCESS_TOKEN_TYPE = "access";

	private static final String REFRESH_TOKEN_TYPE = "refresh";

	private final JwtProperties jwtProperties;

	public JwtTokenProvider(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
	}

	public String generateAccessToken(AuthenticatedUser user) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(jwtProperties.getAccessTokenExpirationMinutes(), ChronoUnit.MINUTES);

		return buildToken(user, now, expiresAt, ACCESS_TOKEN_TYPE);
	}

	public String generateRefreshToken(AuthenticatedUser user) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(jwtProperties.getRefreshTokenExpirationDays(), ChronoUnit.DAYS);

		return buildToken(user, now, expiresAt, REFRESH_TOKEN_TYPE);
	}

	public boolean validateToken(String token) {
		if (!StringUtils.hasText(token)) {
			return false;
		}

		try {
			parseClaims(token);
			return true;
		}
		catch (JwtException | IllegalArgumentException exception) {
			return false;
		}
	}

	public boolean validateRefreshToken(String token) {
		if (!validateToken(token)) {
			return false;
		}

		return REFRESH_TOKEN_TYPE.equals(parseTokenType(token));
	}

	public Long parseUserId(String token) {
		String subject = parseClaims(token).getSubject();
		return Long.valueOf(subject);
	}

	public String parseSubject(String token) {
		return parseClaims(token).getSubject();
	}

	public long getAccessTokenExpiresInSeconds() {
		return jwtProperties.getAccessTokenExpirationMinutes() * 60;
	}

	public List<String> parseAuthorities(String token) {
		Object authorities = parseClaims(token).get(AUTHORITIES_CLAIM);
		if (authorities instanceof List<?> authorityList) {
			return authorityList.stream()
				.filter(String.class::isInstance)
				.map(String.class::cast)
				.toList();
		}

		return List.of();
	}

	private String parseTokenType(String token) {
		return parseClaims(token).get(TOKEN_TYPE_CLAIM, String.class);
	}

	private String buildToken(AuthenticatedUser user, Instant issuedAt, Instant expiresAt, String tokenType) {
		List<String> authorities = user.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.toList();

		return Jwts.builder()
			.subject(user.getId().toString())
			.issuer(jwtProperties.getIssuer())
			.issuedAt(Date.from(issuedAt))
			.expiration(Date.from(expiresAt))
			.claim("email", user.getEmail())
			.claim("username", user.getUsername())
			.claim(AUTHORITIES_CLAIM, authorities)
			.claim(TOKEN_TYPE_CLAIM, tokenType)
			.signWith(signingKey(), Jwts.SIG.HS256)
			.compact();
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
			.verifyWith(signingKey())
			.requireIssuer(jwtProperties.getIssuer())
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

	private SecretKey signingKey() {
		String secret = jwtProperties.getSecret();
		if (!StringUtils.hasText(secret)) {
			throw new IllegalStateException("JWT secret must be configured");
		}

		try {
			return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		}
		catch (WeakKeyException exception) {
			throw new IllegalStateException("JWT secret must be at least 256 bits for HS256", exception);
		}
	}
}
