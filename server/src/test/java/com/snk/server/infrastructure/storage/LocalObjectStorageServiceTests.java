package com.snk.server.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
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
			createPngBytes(640, 360)
		);

		StoredObject storedObject = storageService.storeImage(file);

		Path storedFile = tempDir.resolve(storedObject.objectKey());
		Path thumbnailFile = tempDir.resolve(storedObject.thumbnailObjectKey());
		assertThat(storedObject.resourceUrl()).startsWith("/uploads/images/");
		assertThat(storedObject.thumbnailUrl()).startsWith("/uploads/images/");
		assertThat(storedObject.contentType()).isEqualTo("image/png");
		assertThat(storedObject.size()).isEqualTo(file.getSize());
		assertThat(Files.exists(storedFile)).isTrue();
		assertThat(Files.exists(thumbnailFile)).isTrue();
		assertThat(Files.size(thumbnailFile)).isGreaterThan(0L);
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
			createPngBytes(640, 360)
		);

		StoredObject storedObject = storageService.storeImage(file);

		assertThat(storedObject.resourceUrl()).startsWith("https://snk.qiuxinmin.cn/uploads/images/");
		assertThat(storedObject.thumbnailUrl()).startsWith("https://snk.qiuxinmin.cn/uploads/images/");
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

	private byte[] createPngBytes(int width, int height) {
		try {
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					image.setRGB(x, y, Color.ORANGE.getRGB());
				}
			}
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ImageIO.write(image, "png", outputStream);
			return outputStream.toByteArray();
		}
		catch (Exception exception) {
			throw new IllegalStateException("Failed to build test png bytes.", exception);
		}
	}
}
