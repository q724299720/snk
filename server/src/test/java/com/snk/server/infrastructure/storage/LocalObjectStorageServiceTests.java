package com.snk.server.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class LocalObjectStorageServiceTests {

	@TempDir
	Path tempDir;

	@Test
	void shouldStoreImageUnderConfiguredRoot() throws Exception {
		StorageProperties properties = new StorageProperties();
		properties.setRootPath(tempDir.toString());
		properties.setPublicPath("/uploads");
		LocalObjectStorageService storageService = new LocalObjectStorageService(properties);

		MockMultipartFile file = new MockMultipartFile(
			"file",
			"snk.png",
			"image/png",
			"png-content".getBytes()
		);

		StoredObject storedObject = storageService.storeImage(file);

		Path storedFile = tempDir.resolve(storedObject.objectKey());
		assertThat(storedObject.resourceUrl()).startsWith("/uploads/images/");
		assertThat(storedObject.contentType()).isEqualTo("image/png");
		assertThat(storedObject.size()).isEqualTo(file.getSize());
		assertThat(Files.exists(storedFile)).isTrue();
		assertThat(Files.readAllBytes(storedFile)).isEqualTo("png-content".getBytes());
	}

	@Test
	void shouldBuildAbsoluteResourceUrlWhenPublicBaseUrlConfigured() {
		StorageProperties properties = new StorageProperties();
		properties.setRootPath(tempDir.toString());
		properties.setPublicPath("/uploads");
		properties.setPublicBaseUrl("https://snk.qiuxinmin.cn/");
		LocalObjectStorageService storageService = new LocalObjectStorageService(properties);

		MockMultipartFile file = new MockMultipartFile(
			"file",
			"snk.png",
			"image/png",
			"png-content".getBytes()
		);

		StoredObject storedObject = storageService.storeImage(file);

		assertThat(storedObject.resourceUrl()).startsWith("https://snk.qiuxinmin.cn/uploads/images/");
	}

	@Test
	void shouldRejectNonImageUpload() {
		StorageProperties properties = new StorageProperties();
		properties.setRootPath(tempDir.toString());
		LocalObjectStorageService storageService = new LocalObjectStorageService(properties);

		MockMultipartFile file = new MockMultipartFile(
			"file",
			"notes.txt",
			"text/plain",
			"hello".getBytes()
		);

		assertThatThrownBy(() -> storageService.storeImage(file))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining("Only image uploads are supported");
	}
}
