package dev.lucasxie.learning.file.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import dev.lucasxie.learning.common.exception.BusinessException;
import dev.lucasxie.learning.common.exception.ErrorCode;
import dev.lucasxie.learning.file.FileAccessProperties;

@Service
public class FileAccessTokenService {

	private static final String HMAC_ALGORITHM = "HmacSHA256";

	private static final String TOKEN_SEPARATOR = ".";

	private final FileAccessProperties fileAccessProperties;

	public FileAccessTokenService(FileAccessProperties fileAccessProperties) {
		this.fileAccessProperties = fileAccessProperties;
	}

	public String generateToken(Long fileId, Instant expiresAt) {
		String payload = encodePayload(fileId, expiresAt);
		return payload + TOKEN_SEPARATOR + sign(payload);
	}

	public void validateToken(Long fileId, String token) {
		if (!StringUtils.hasText(token)) {
			throw new BusinessException(ErrorCode.FILE_ACCESS_TOKEN_INVALID, "File access token is required");
		}

		String[] parts = token.split("\\.", 2);
		if (parts.length != 2 || !StringUtils.hasText(parts[0]) || !StringUtils.hasText(parts[1])) {
			throw new BusinessException(ErrorCode.FILE_ACCESS_TOKEN_INVALID, "File access token is invalid");
		}

		if (!constantTimeEquals(sign(parts[0]), parts[1])) {
			throw new BusinessException(ErrorCode.FILE_ACCESS_TOKEN_INVALID, "File access token is invalid");
		}

		TokenPayload payload = decodePayload(parts[0]);
		if (!fileId.equals(payload.fileId())) {
			throw new BusinessException(ErrorCode.FILE_ACCESS_TOKEN_INVALID, "File access token is invalid for this file");
		}

		if (Instant.now().isAfter(payload.expiresAt())) {
			throw new BusinessException(ErrorCode.FILE_ACCESS_TOKEN_INVALID, "File access token has expired");
		}
	}

	private String encodePayload(Long fileId, Instant expiresAt) {
		String payload = fileId + ":" + expiresAt.getEpochSecond();
		return Base64.getUrlEncoder()
			.withoutPadding()
			.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
	}

	private TokenPayload decodePayload(String encodedPayload) {
		try {
			String payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
			String[] parts = payload.split(":", 2);
			if (parts.length != 2) {
				throw new IllegalArgumentException("Invalid payload");
			}

			return new TokenPayload(Long.valueOf(parts[0]), Instant.ofEpochSecond(Long.parseLong(parts[1])));
		}
		catch (IllegalArgumentException exception) {
			throw new BusinessException(ErrorCode.FILE_ACCESS_TOKEN_INVALID, "File access token is invalid");
		}
	}

	private String sign(String payload) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(fileAccessProperties.getSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
			return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception exception) {
			throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "Failed to sign file access token");
		}
	}

	private boolean constantTimeEquals(String expected, String actual) {
		return MessageDigestEqual.constantTimeEquals(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
	}

	private record TokenPayload(Long fileId, Instant expiresAt) {
	}

	private static final class MessageDigestEqual {

		private static boolean constantTimeEquals(byte[] expected, byte[] actual) {
			if (expected.length != actual.length) {
				return false;
			}

			int result = 0;
			for (int i = 0; i < expected.length; i++) {
				result |= expected[i] ^ actual[i];
			}
			return result == 0;
		}
	}
}
