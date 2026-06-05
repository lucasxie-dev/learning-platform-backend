package dev.lucasxie.learning.dashboard.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.lucasxie.learning.common.api.ApiResponse;
import dev.lucasxie.learning.dashboard.dto.DashboardResponse;
import dev.lucasxie.learning.dashboard.service.DashboardService;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

	private final DashboardService dashboardService;

	public DashboardController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@GetMapping
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<DashboardResponse> getDashboard() {
		return ApiResponse.success(dashboardService.getDashboard());
	}
}
