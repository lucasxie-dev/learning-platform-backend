package dev.lucasxie.learning.settings.dto;

public record MediaStorageSettingsResponse(
	String storageProvider,
	Long maxFileSizeMb,
	Boolean signedAccessEnabled,
	Long signedUrlExpirationMinutes,
	Boolean databaseStorageForDemo,
	String productionRecommendation
) {
}
