package com.snk.server.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LocalObjectStorageService implements ObjectStorageService {

	private static final DateTimeFormatter PARTITION_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM");

	private final StorageProperties storageProperties;
	private final Path rootPath;

	public LocalObjectStorageService(StorageProperties storageProperties) {
		this.storageProperties = storageProperties;
		this.rootPath = Path.of(storageProperties.getRootPath()).toAbsolutePath().normalize();
		try {
			Files.createDirectories(this.rootPath);
		}
		catch (IOException exception) {
			throw new IllegalStateException("Failed to initialize local storage root: " + this.rootPath, exception);
		}
	}

	@Override
	public StoredObject storeImage(MultipartFile file) {
		validateImage(file);

		String extension = resolveExtension(file);
		String partition = OffsetDateTime.now().format(PARTITION_FORMAT);
		String objectKey = String.format(
			Locale.ROOT,
			"%s/%s/%s%s",
			storageProperties.getImageDirectory(),
			partition,
			UUID.randomUUID(),
			extension
		).replace("\\", "/");

		Path targetFile = rootPath.resolve(objectKey).normalize();
		if (!targetFile.startsWith(rootPath)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid storage target path.");
		}

		try {
			Files.createDirectories(targetFile.getParent());
			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch (IOException exception) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store image.", exception);
		}

		String resourceUrl = normalizePublicPath(storageProperties.getPublicPath()) + "/" + objectKey;
		return new StoredObject(objectKey, resourceUrl, file.getContentType(), file.getSize());
	}

	private void validateImage(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is required.");
		}

		if (file.getSize() > storageProperties.getMaxImageSizeBytes()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is too large.");
		}

		String contentType = file.getContentType();
		if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image uploads are supported.");
		}
	}

	private String resolveExtension(MultipartFile file) {
		String originalFilename = file.getOriginalFilename();
		if (originalFilename == null) {
			return "";
		}

		int dotIndex = originalFilename.lastIndexOf('.');
		if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
			return "";
		}

		String extension = originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
		if (!extension.matches("\\.[a-z0-9]{1,10}")) {
			return "";
		}

		return extension;
	}

	private String normalizePublicPath(String publicPath) {
		String normalized = publicPath == null || publicPath.isBlank() ? "/uploads" : publicPath.trim();
		if (!normalized.startsWith("/")) {
			normalized = "/" + normalized;
		}
		if (normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		return normalized;
	}
}
