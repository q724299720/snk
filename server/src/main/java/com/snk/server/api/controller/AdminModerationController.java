package com.snk.server.api.controller;

import com.snk.server.api.dto.AutoAuditSummaryResponse;
import com.snk.server.domain.food.FoodModerationAutoAuditService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/moderation")
public class AdminModerationController {

	private final FoodModerationAutoAuditService foodModerationAutoAuditService;

	public AdminModerationController(FoodModerationAutoAuditService foodModerationAutoAuditService) {
		this.foodModerationAutoAuditService = foodModerationAutoAuditService;
	}

	@PostMapping("/auto-audit/run")
	public AutoAuditSummaryResponse runAutoAudit() {
		return AutoAuditSummaryResponse.from(foodModerationAutoAuditService.runAutoAudit());
	}
}
