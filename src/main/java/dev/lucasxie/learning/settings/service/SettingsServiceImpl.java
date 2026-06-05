package dev.lucasxie.learning.settings.service;

import java.util.Collection;
import java.util.List;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.lucasxie.learning.file.FileAccessProperties;
import dev.lucasxie.learning.file.StorageProvider;
import dev.lucasxie.learning.settings.ProjectInfoProperties;
import dev.lucasxie.learning.settings.dto.MediaStorageSettingsResponse;
import dev.lucasxie.learning.settings.dto.ProfileSettingsResponse;
import dev.lucasxie.learning.settings.dto.SettingsOverviewResponse;
import dev.lucasxie.learning.settings.dto.SystemSettingsResponse;
import dev.lucasxie.learning.storage.StorageProperties;
import dev.lucasxie.learning.user.dto.CurrentUserResponse;
import dev.lucasxie.learning.user.service.UserQueryService;

@Service
public class SettingsServiceImpl implements SettingsService {

	private static final String API_VERSION = "v1";

	private static final String SWAGGER_URL = "/swagger-ui/index.html";

	private static final String OPEN_API_URL = "/v3/api-docs";

	private static final String DEFAULT_APPLICATION_NAME = "learning-platform-backend";

	private static final String DEFAULT_ENVIRONMENT = "local";

	private static final String PRODUCTION_RECOMMENDATION =
		"Database storage is intended for local demo and small files. Use MinIO or S3 for production-like media delivery.";

	private final UserQueryService userQueryService;

	private final StorageProperties storageProperties;

	private final FileAccessProperties fileAccessProperties;

	private final ProjectInfoProperties projectInfoProperties;

	private final Environment environment;

	public SettingsServiceImpl(
		UserQueryService userQueryService,
		StorageProperties storageProperties,
		FileAccessProperties fileAccessProperties,
		ProjectInfoProperties projectInfoProperties,
		Environment environment
	) {
		this.userQueryService = userQueryService;
		this.storageProperties = storageProperties;
		this.fileAccessProperties = fileAccessProperties;
		this.projectInfoProperties = projectInfoProperties;
		this.environment = environment;
	}

	@Override
	@Transactional(readOnly = true)
	public SettingsOverviewResponse getOverview() {
		return new SettingsOverviewResponse(
			buildProfile(userQueryService.getCurrentUser()),
			buildSystem(),
			buildMediaStorage()
		);
	}

	private ProfileSettingsResponse buildProfile(CurrentUserResponse currentUser) {
		return new ProfileSettingsResponse(
			currentUser.id(),
			currentUser.email(),
			currentUser.username(),
			currentUser.displayName(),
			currentUser.avatarUrl(),
			currentUser.status().name(),
			toSortedList(currentUser.roles()),
			toSortedList(currentUser.permissions())
		);
	}

	private SystemSettingsResponse buildSystem() {
		return new SystemSettingsResponse(
			environment.getProperty("spring.application.name", DEFAULT_APPLICATION_NAME),
			resolveEnvironmentName(),
			API_VERSION,
			SWAGGER_URL,
			OPEN_API_URL,
			projectInfoProperties.getBackendRepository(),
			projectInfoProperties.getFrontendRepository()
		);
	}

	private MediaStorageSettingsResponse buildMediaStorage() {
		StorageProvider provider = storageProperties.getProvider() == null
			? StorageProvider.DATABASE
			: storageProperties.getProvider();

		return new MediaStorageSettingsResponse(
			provider.name(),
			storageProperties.getMaxFileSizeMb(),
			true,
			fileAccessProperties.getDefaultExpirationMinutes(),
			provider == StorageProvider.DATABASE,
			PRODUCTION_RECOMMENDATION
		);
	}

	private String resolveEnvironmentName() {
		String[] activeProfiles = environment.getActiveProfiles();
		if (activeProfiles.length == 0) {
			return DEFAULT_ENVIRONMENT;
		}

		return String.join(",", activeProfiles);
	}

	private List<String> toSortedList(Collection<String> values) {
		return values == null
			? List.of()
			: values.stream()
				.sorted()
				.toList();
	}
}
