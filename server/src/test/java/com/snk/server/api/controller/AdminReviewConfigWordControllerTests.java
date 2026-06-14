package com.snk.server.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snk.server.domain.review.CreateReviewConfigWordCommand;
import com.snk.server.domain.review.ReviewConfigWordService;
import com.snk.server.domain.review.UpdateReviewConfigWordCommand;
import com.snk.server.infrastructure.storage.StorageProperties;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@Import(AdminReviewConfigWordControllerTests.ControllerTestConfiguration.class)
@WebMvcTest(AdminReviewConfigWordController.class)
class AdminReviewConfigWordControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ReviewConfigWordService reviewConfigWordService;

	@TestConfiguration
	static class ControllerTestConfiguration {

		@Bean
		StorageProperties storageProperties() {
			return new StorageProperties();
		}
	}

	@Test
	void shouldListWords() throws Exception {
		when(reviewConfigWordService.listWords(eq(true), eq("valid_food_word")))
			.thenReturn(List.of(wordItem(1L, "apple", "valid_food_word", true)));

		mockMvc.perform(get("/api/admin/review-config-words")
			.param("enabled", "true")
			.param("wordType", "valid_food_word"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].word").value("apple"))
			.andExpect(jsonPath("$[0].enabled").value(true));
	}

	@Test
	void shouldCreateWord() throws Exception {
		when(reviewConfigWordService.createWord(any(CreateReviewConfigWordCommand.class)))
			.thenReturn(wordItem(2L, "banana", "valid_food_word", true));

		mockMvc.perform(post("/api/admin/review-config-words")
			.contentType("application/json")
			.content("""
				{
				  "word": "banana",
				  "wordType": "valid_food_word",
				  "source": "manual",
				  "remark": "fruit",
				  "enabled": true,
				  "operatorId": "admin-1",
				  "operatorName": "Admin"
				}
				"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.word").value("banana"))
			.andExpect(jsonPath("$.enabled").value(true));
	}

	@Test
	void shouldUpdateWord() throws Exception {
		when(reviewConfigWordService.updateWord(any(UpdateReviewConfigWordCommand.class)))
			.thenReturn(wordItem(3L, "melon", "valid_food_word", false));

		mockMvc.perform(put("/api/admin/review-config-words/3")
			.contentType("application/json")
			.content("""
				{
				  "word": "melon",
				  "wordType": "valid_food_word",
				  "source": "manual",
				  "remark": "updated",
				  "operatorId": "admin-2",
				  "operatorName": "Moderator"
				}
				"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.word").value("melon"))
			.andExpect(jsonPath("$.enabled").value(false));
	}

	@Test
	void shouldEnableWord() throws Exception {
		when(reviewConfigWordService.enableWord(4L, "admin-3", "Operator"))
			.thenReturn(wordItem(4L, "pear", "valid_food_word", true));

		mockMvc.perform(post("/api/admin/review-config-words/4/enable")
			.contentType("application/json")
			.content("""
				{
				  "operatorId": "admin-3",
				  "operatorName": "Operator"
				}
				"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.enabled").value(true));
	}

	@Test
	void shouldDisableWord() throws Exception {
		when(reviewConfigWordService.disableWord(6L, "admin-4", "Operator"))
			.thenReturn(wordItem(6L, "plum", "valid_food_word", false));

		mockMvc.perform(post("/api/admin/review-config-words/6/disable")
			.contentType("application/json")
			.content("""
				{
				  "operatorId": "admin-4",
				  "operatorName": "Operator"
				}
				"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.enabled").value(false));
	}

	@Test
	void shouldReturnAuditLogs() throws Exception {
		when(reviewConfigWordService.listAuditLogs(5L))
			.thenReturn(List.of(auditItem(10L, 5L, "create")));

		mockMvc.perform(get("/api/admin/review-config-words/5/audit-logs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].actionType").value("create"))
			.andExpect(jsonPath("$[0].afterValue.word").value("apple"));
	}

	private ReviewConfigWordService.ReviewConfigWordItem wordItem(Long id, String word, String wordType, boolean enabled) {
		return new ReviewConfigWordService.ReviewConfigWordItem(
			id,
			word,
			wordType,
			enabled,
			"manual",
			"note",
			"admin",
			OffsetDateTime.parse("2026-06-14T12:00:00Z"),
			OffsetDateTime.parse("2026-06-14T12:00:00Z")
		);
	}

	private ReviewConfigWordService.ReviewConfigWordAuditLogItem auditItem(Long id, Long wordId, String actionType) {
		return new ReviewConfigWordService.ReviewConfigWordAuditLogItem(
			id,
			wordId,
			actionType,
			null,
			Map.of("id", wordId, "word", "apple", "enabled", true),
			"admin-1",
			"Admin",
			OffsetDateTime.parse("2026-06-14T12:00:00Z")
		);
	}
}
