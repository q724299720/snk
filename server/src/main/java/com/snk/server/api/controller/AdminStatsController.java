package com.snk.server.api.controller;

import com.snk.server.api.dto.AdminStatsResponse;
import com.snk.server.domain.admin.AdminStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

	private final AdminStatsService adminStatsService;

	public AdminStatsController(AdminStatsService adminStatsService) {
		this.adminStatsService = adminStatsService;
	}

	@GetMapping
	public AdminStatsResponse getStats() {
		return AdminStatsResponse.from(adminStatsService.getStats());
	}
}
