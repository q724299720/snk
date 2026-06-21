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
		assertThat(html).contains("function renderFoodItemDetail");
		assertThat(html).contains("function renderFoodItemReports");
		assertThat(html).contains("/api/admin/food-items/${id}");
		assertThat(html).contains("/api/admin/food-items/${id}/reports");
	}

	@Test
	void adminConsoleExposesRecognitionTaskMonitorFlow() throws Exception {
		String html = readAdminConsoleHtml();

		assertThat(html).contains("id=\"recognitionTasks\"");
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
