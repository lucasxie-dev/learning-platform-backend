package dev.lucasxie.learning.settings.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import dev.lucasxie.learning.common.api.ApiResponse;
import dev.lucasxie.learning.settings.dto.MediaStorageSettingsResponse;
import dev.lucasxie.learning.settings.dto.ProfileSettingsResponse;
import dev.lucasxie.learning.settings.dto.SettingsOverviewResponse;
import dev.lucasxie.learning.settings.dto.SystemSettingsResponse;
import dev.lucasxie.learning.settings.service.SettingsService;

class SettingsControllerTest {

	@Test
	void getOverviewIsMappedToAuthenticatedSettingsOverviewEndpoint() throws NoSuchMethodException {
		RequestMapping requestMapping = SettingsController.class.getAnnotation(RequestMapping.class);
		Method method = SettingsController.class.getMethod("getOverview");
		GetMapping getMapping = method.getAnnotation(GetMapping.class);
		PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

		assertThat(requestMapping.value()).containsExactly("/api/v1/settings");
		assertThat(getMapping.value()).containsExactly("/overview");
		assertThat(preAuthorize.value()).isEqualTo("isAuthenticated()");
		assertThat(preAuthorize.value()).doesNotContain("ADMIN");
	}

	@Test
	void getOverviewReturnsApiResponseWithSettingsOverviewData() {
		SettingsOverviewResponse overview = new SettingsOverviewResponse(
			new ProfileSettingsResponse(
				1L,
				"lucasxie.dev@gmail.com",
				"lucas",
				"Lucas",
				null,
				"ACTIVE",
				List.of("TEACHER"),
				List.of("file:upload")
			),
			new SystemSettingsResponse(
				"learning-platform-backend",
				"local",
				"v1",
				"/swagger-ui/index.html",
				"/v3/api-docs",
				"https://github.com/lucasxie-dev/learning-platform-backend",
				"https://github.com/lucasxie-dev/learning-platform-web"
			),
			new MediaStorageSettingsResponse(
				"DATABASE",
				20L,
				true,
				10L,
				true,
				"Database storage is intended for local demo and small files. Use MinIO or S3 for production-like media delivery."
			)
		);

		SettingsService settingsService = mock(SettingsService.class);
		when(settingsService.getOverview()).thenReturn(overview);
		SettingsController controller = new SettingsController(settingsService);

		ApiResponse<SettingsOverviewResponse> response = controller.getOverview();

		assertThat(response.isSuccess()).isTrue();
		assertThat(response.getCode()).isEqualTo("SUCCESS");
		assertThat(response.getData()).isSameAs(overview);
	}
}
