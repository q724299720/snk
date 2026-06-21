package com.snk.server.infrastructure.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.util.StreamUtils;

class AdminConsoleStaticPageTests {

	@Test
	void adminConsoleExposesReviewWordEditFlow() throws Exception {
		String html = readAdminConsoleHtml();

		assertThat(html).contains("data-word-action=\"edit\"");
		assertThat(html).contains("id=\"saveWord\"");
		assertThat(html).contains("function startEditWord");
		assertThat(html).contains("async function updateWord");
		assertThat(html).contains("method: \"PUT\"");
		assertThat(html).contains("/api/admin/review-config-words/${id}");
	}

	@Test
	void adminConsoleExposesFoodItemDetailFlow() throws Exception {
		String html = readAdminConsoleHtml();

		assertThat(html).contains("id=\"foodItemDetail\"");
		assertThat(html).contains("data-action=\"detail\"");
		assertThat(html).contains("async function loadFoodItemDetail");
		assertThat(html).contains("async function loadFoodItemReports");
		assertThat(html).contains("await Promise.all([loadStats(), loadFoodItems(), loadFoodItemDetail(id)])");
		assertThat(html).contains("function renderFoodItemDetail");
		assertThat(html).contains("function renderFoodItemReports");
		assertThat(html).contains("id=\"mergeTargetFoodItemId\"");
		assertThat(html).contains("id=\"previewMergeTarget\"");
		assertThat(html).contains("id=\"mergeTargetPreview\"");
		assertThat(html).contains("async function mergeFoodItem");
		assertThat(html).contains("async function previewMergeTarget");
		assertThat(html).contains("function clearMergeTargetPreview");
		assertThat(html).contains("clearMergeTargetPreview();");
		assertThat(html).contains("let mergeTargetPreviewItem = null");
		assertThat(html).contains("mergeTargetPreviewItem.auditStatus !== \"approved\"");
		assertThat(html).contains("mergeTargetPreviewItem.id !== targetFoodItemId");
		assertThat(html).contains("targetFoodItemId === Number(id)");
		assertThat(html).contains("不能将条目合并到自己");
		assertThat(html).contains("/api/admin/food-items/${id}/merge");
		assertThat(html).contains("/api/admin/food-items/${id}");
		assertThat(html).contains("/api/admin/food-items/${id}/reports");
	}

	@Test
	void adminConsoleExposesManualAutoAuditFlow() throws Exception {
		String html = readAdminConsoleHtml();

		assertThat(html).contains("id=\"runAutoAudit\"");
		assertThat(html).contains("async function runAutoAudit");
		assertThat(html).contains("/api/admin/moderation/auto-audit/run");
		assertThat(html).contains("rejectedFoodItemIds");
	}

	@Test
	void adminConsoleExposesRecognitionTaskMonitorFlow() throws Exception {
		String html = readAdminConsoleHtml();

		assertThat(html).contains("id=\"recognitionTasks\"");
		assertThat(html).contains("pendingRecognitionTasks");
		assertThat(html).contains("data-task-action=\"detail\"");
		assertThat(html).contains("async function loadRecognitionTasks");
		assertThat(html).contains("async function loadRecognitionTaskDetail");
		assertThat(html).contains("function renderRecognitionTasks");
		assertThat(html).contains("function renderRecognitionTaskDetail");
		assertThat(html).contains("/api/admin/recognition-tasks");
		assertThat(html).contains("/api/admin/recognition-tasks/${id}");
	}

	private String readAdminConsoleHtml() throws Exception {
		try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("static/admin/index.html")) {
			assertThat(inputStream).as("static admin console page").isNotNull();
			return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
		}
	}
}
