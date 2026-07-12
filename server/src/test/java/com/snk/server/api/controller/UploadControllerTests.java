package com.snk.server.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.snk.server.infrastructure.storage.StorageProperties;
import com.snk.server.infrastructure.storage.LocalObjectStorageService;
import com.snk.server.infrastructure.storage.ObjectStorageService;
import com.snk.server.infrastructure.storage.StoredObject;
import com.snk.server.infrastructure.persistence.auth.UploadedObjectRepository;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import com.snk.server.infrastructure.security.CurrentUser;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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

	@TempDir
	Path tempDir;

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ObjectStorageService objectStorageService;
	@MockBean private UploadedObjectRepository uploadedObjects;
	@MockBean private UserRepository users;
	@MockBean private CurrentUser currentUser;

	@org.junit.jupiter.api.BeforeEach
	void useTrustedAccountIdentity() { when(currentUser.requiredUserId()).thenReturn(2L); }

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
			new StoredObject(
				"images/2026/06/test.png",
				"/uploads/images/2026/06/test.png",
				"images/2026/06/test_thumb.png",
				"/uploads/images/2026/06/test_thumb.png",
				"image/png",
				11L
			)
		);

		mockMvc.perform(multipart("/api/upload/image").file(file))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.objectKey").value("images/2026/06/test.png"))
			.andExpect(jsonPath("$.resourceUrl").value("/uploads/images/2026/06/test.png"))
			.andExpect(jsonPath("$.thumbnailObjectKey").value("images/2026/06/test_thumb.png"))
			.andExpect(jsonPath("$.thumbnailUrl").value("/uploads/images/2026/06/test_thumb.png"))
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

	@Test
	void shouldDeleteStoredFilesWhenMetadataPersistenceFails() throws Exception {
		StorageProperties properties = new StorageProperties();
		properties.setRootPath(tempDir.toString());
		LocalObjectStorageService localStorage = new LocalObjectStorageService(properties);
		MockMultipartFile file = new MockMultipartFile("file", "snk.png", "image/png", createPngBytes());
		when(users.getReferenceById(2L)).thenReturn(new UserEntity());
		when(uploadedObjects.save(any())).thenThrow(new IllegalStateException("metadata unavailable"));

		UploadController controller = new UploadController(localStorage, uploadedObjects, users, currentUser);

		assertThatThrownBy(() -> controller.uploadImage(file))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("metadata unavailable");

		try (Stream<Path> paths = Files.walk(tempDir)) {
			assertThat(paths.filter(Files::isRegularFile)).isEmpty();
		}
	}

	private byte[] createPngBytes() throws Exception {
		BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(image, "png", output);
		return output.toByteArray();
	}
}
