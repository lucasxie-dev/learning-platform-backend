package dev.lucasxie.learning.settings.dto;

public record SettingsOverviewResponse(
	ProfileSettingsResponse profile,
	SystemSettingsResponse system,
	MediaStorageSettingsResponse mediaStorage
) {
}
