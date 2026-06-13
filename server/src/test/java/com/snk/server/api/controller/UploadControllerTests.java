package com.snk.server.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snk.server.infrastructure.storage.StorageProperties;
import com.snk.server.infrastructure.storage.ObjectStorageService;
import com.snk.server.infrastructure.storage.StoredObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

@Import(UploadControllerTests.ControllerTestConfiguration.class)
@WebMvcTest(UploadController.class)
class UploadControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ObjectStorageService objectStorageService;

	@TestConfiguration
	static class ControllerTestConfiguration {

		@Bean
		StorageProperties storageProperties() {
			return new StorageProperties();
		}
	}

	@Test
	void shouldUploadImage() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"snk.png",
			"image/png",
			"png-content".getBytes()
		);

		when(objectStorageService.storeImage(any())).thenReturn(
			new StoredObject("images/2026/06/test.png", "/uploads/images/2026/06/test.png", "image/png", 11L)
		);

		mockMvc.perform(multipart("/api/upload/image").file(file))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.objectKey").value("images/2026/06/test.png"))
			.andExpect(jsonPath("$.resourceUrl").value("/uploads/images/2026/06/test.png"))
			.andExpect(jsonPath("$.contentType").value("image/png"))
			.andExpect(jsonPath("$.size").value(11));
	}

	@Test
	void shouldReturnBadRequestWhenStorageRejectsFile() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"bad.txt",
			"text/plain",
			"bad".getBytes()
		);

		when(objectStorageService.storeImage(any())).thenThrow(
			new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image uploads are supported.")
		);

		mockMvc.perform(multipart("/api/upload/image").file(file))
			.andExpect(status().isBadRequest());
	}
}
