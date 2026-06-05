package dev.lucasxie.learning.settings.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.lucasxie.learning.common.api.ApiResponse;
import dev.lucasxie.learning.settings.dto.SettingsOverviewResponse;
import dev.lucasxie.learning.settings.service.SettingsService;

@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

	private final SettingsService settingsService;

	public SettingsController(SettingsService settingsService) {
		this.settingsService = settingsService;
	}

	@GetMapping("/overview")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<SettingsOverviewResponse> getOverview() {
		return ApiResponse.success(settingsService.getOverview());
	}
}
