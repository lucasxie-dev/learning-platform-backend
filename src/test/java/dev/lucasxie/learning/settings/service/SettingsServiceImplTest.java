package dev.lucasxie.learning.settings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import dev.lucasxie.learning.file.FileAccessProperties;
import dev.lucasxie.learning.file.StorageProvider;
import dev.lucasxie.learning.settings.ProjectInfoProperties;
import dev.lucasxie.learning.settings.dto.MediaStorageSettingsResponse;
import dev.lucasxie.learning.settings.dto.ProfileSettingsResponse;
import dev.lucasxie.learning.settings.dto.SettingsOverviewResponse;
import dev.lucasxie.learning.settings.dto.SystemSettingsResponse;
import dev.lucasxie.learning.storage.StorageProperties;
import dev.lucasxie.learning.user.UserStatus;
import dev.lucasxie.learning.user.dto.CurrentUserResponse;
import dev.lucasxie.learning.user.service.UserQueryService;

class SettingsServiceImplTest {

	@Test
	void getOverviewReturnsProfileInformationForAuthenticatedUser() {
		SettingsOverviewResponse response = createService().getOverview();

		assertThat(response.profile().id()).isEqualTo(1L);
		assertThat(response.profile().email()).isEqualTo("lucasxie.dev@gmail.com");
		assertThat(response.profile().username()).isEqualTo("lucas");
		assertThat(response.profile().displayName()).isEqualTo("Lucas");
		assertThat(response.profile().avatarUrl()).isNull();
		assertThat(response.profile().status()).isEqualTo("ACTIVE");
	}

	@Test
	void getOverviewIncludesRolesAndPermissions() {
		SettingsOverviewResponse response = createService().getOverview();

		assertThat(response.profile().roles()).containsExactly("STUDENT", "TEACHER");
		assertThat(response.profile().permissions()).containsExactly("course:create", "course:update", "file:upload");
	}

	@Test
	void getOverviewReturnsSafeSystemMetadata() {
		SettingsOverviewResponse response = createService().getOverview();

		assertThat(response.system().applicationName()).isEqualTo("learning-platform-backend");
		assertThat(response.system().environment()).isEqualTo("local");
		assertThat(response.system().apiVersion()).isEqualTo("v1");
		assertThat(response.system().swaggerUrl()).isEqualTo("/swagger-ui/index.html");
		assertThat(response.system().openApiUrl()).isEqualTo("/v3/api-docs");
		assertThat(response.system().backendRepository()).isEqualTo("https://github.com/lucasxie-dev/learning-platform-backend");
		assertThat(response.system().frontendRepository()).isEqualTo("https://github.com/lucasxie-dev/learning-platform-web");
	}

	@Test
	void getOverviewReturnsMediaStorageSettings() {
		SettingsOverviewResponse response = createService().getOverview();

		assertThat(response.mediaStorage().storageProvider()).isEqualTo("DATABASE");
		assertThat(response.mediaStorage().maxFileSizeMb()).isEqualTo(20);
		assertThat(response.mediaStorage().signedAccessEnabled()).isTrue();
		assertThat(response.mediaStorage().signedUrlExpirationMinutes()).isEqualTo(10);
		assertThat(response.mediaStorage().databaseStorageForDemo()).isTrue();
		assertThat(response.mediaStorage().productionRecommendation()).contains("Use MinIO or S3");
	}

	@Test
	void responseDtosDoNotDeclareSensitiveFields() {
		Set<String> fieldNames = Stream.of(
				SettingsOverviewResponse.class,
				ProfileSettingsResponse.class,
				SystemSettingsResponse.class,
				MediaStorageSettingsResponse.class
			)
			.flatMap(type -> Arrays.stream(type.getRecordComponents()))
			.map(RecordComponent::getName)
			.map(String::toLowerCase)
			.collect(java.util.stream.Collectors.toSet());

		assertThat(fieldNames).doesNotContain(
			"jwtsecret",
			"databaspassword",
			"databasepassword",
			"miniokey",
			"miniosecretkey",
			"accesskey",
			"secretkey",
			"accesstoken",
			"refreshtoken",
			"token",
			"password",
			"secret",
			"environmentvariables"
		);
	}

	private SettingsService createService() {
		UserQueryService userQueryService = mock(UserQueryService.class);
		when(userQueryService.getCurrentUser()).thenReturn(new CurrentUserResponse(
			1L,
			"lucasxie.dev@gmail.com",
			"lucas",
			"Lucas",
			null,
			UserStatus.ACTIVE,
			Set.of("STUDENT", "TEACHER"),
			Set.of("file:upload", "course:update", "course:create")
		));

		StorageProperties storageProperties = new StorageProperties();
		storageProperties.setProvider(StorageProvider.DATABASE);
		storageProperties.setMaxFileSizeMb(20);
		storageProperties.setPublicBaseUrl("http://localhost:8080");

		FileAccessProperties fileAccessProperties = new FileAccessProperties();
		fileAccessProperties.setDefaultExpirationMinutes(10);

		ProjectInfoProperties projectInfoProperties = new ProjectInfoProperties();
		projectInfoProperties.setBackendRepository("https://github.com/lucasxie-dev/learning-platform-backend");
		projectInfoProperties.setFrontendRepository("https://github.com/lucasxie-dev/learning-platform-web");

		MockEnvironment environment = new MockEnvironment()
			.withProperty("spring.application.name", "learning-platform-backend");

		SettingsService service = new SettingsServiceImpl(
			userQueryService,
			storageProperties,
			fileAccessProperties,
			projectInfoProperties,
			environment
		);

		return service;
	}
}
