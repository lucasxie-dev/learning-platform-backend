package dev.lucasxie.learning.dashboard.dto;

import java.time.Instant;

public record DashboardActivityResponse(
	String type,
	String title,
	String description,
	Instant occurredAt
) {
}
