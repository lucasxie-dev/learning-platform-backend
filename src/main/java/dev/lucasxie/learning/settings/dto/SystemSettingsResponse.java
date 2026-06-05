package dev.lucasxie.learning.settings.dto;

public record SystemSettingsResponse(
	String applicationName,
	String environment,
	String apiVersion,
	String swaggerUrl,
	String openApiUrl,
	String backendRepository,
	String frontendRepository
) {
}
