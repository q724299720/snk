package com.snk.server.api.dto;

import com.snk.server.domain.review.ReviewConfigWordService.ReviewConfigWordAuditLogItem;
import java.time.OffsetDateTime;
import java.util.Map;

public record ReviewConfigWordAuditLogResponse(
	Long id,
	Long reviewConfigWordId,
	String actionType,
	Map<String, Object> beforeValue,
	Map<String, Object> afterValue,
	String operatorId,
	String operatorName,
	OffsetDateTime createdAt
) {
	public static ReviewConfigWordAuditLogResponse from(ReviewConfigWordAuditLogItem item) {
		return new ReviewConfigWordAuditLogResponse(
			item.id(),
			item.reviewConfigWordId(),
			item.actionType(),
			item.beforeValue(),
			item.afterValue(),
			item.operatorId(),
			item.operatorName(),
			item.createdAt()
		);
	}
}
