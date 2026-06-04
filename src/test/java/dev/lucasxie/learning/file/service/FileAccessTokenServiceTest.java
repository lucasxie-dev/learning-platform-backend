package dev.lucasxie.learning.file.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import dev.lucasxie.learning.common.exception.BusinessException;
import dev.lucasxie.learning.common.exception.ErrorCode;
import dev.lucasxie.learning.file.FileAccessProperties;

class FileAccessTokenServiceTest {

	@Test
	void generatedTokenValidatesForSameFileBeforeExpiration() {
		FileAccessTokenService service = createService();
		String token = service.generateToken(1L, Instant.now().plusSeconds(60));

		assertThatCode(() -> service.validateToken(1L, token)).doesNotThrowAnyException();
	}

	@Test
	void tokenValidationRejectsDifferentFile() {
		FileAccessTokenService service = createService();
		String token = service.generateToken(1L, Instant.now().plusSeconds(60));

		assertThatThrownBy(() -> service.validateToken(2L, token))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.FILE_ACCESS_TOKEN_INVALID.name());
	}

	@Test
	void tokenValidationRejectsExpiredToken() {
		FileAccessTokenService service = createService();
		String token = service.generateToken(1L, Instant.now().minusSeconds(1));

		assertThatThrownBy(() -> service.validateToken(1L, token))
			.isInstanceOf(BusinessException.class)
			.extracting("code")
			.isEqualTo(ErrorCode.FILE_ACCESS_TOKEN_INVALID.name());
	}

	private FileAccessTokenService createService() {
		FileAccessProperties properties = new FileAccessProperties();
		properties.setSecret("test-file-access-secret");
		return new FileAccessTokenService(properties);
	}
}
