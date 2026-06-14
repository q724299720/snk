package com.snk.server.infrastructure.storage;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
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
		String thumbnailObjectKey = buildThumbnailObjectKey(objectKey, extension);

		Path targetFile = rootPath.resolve(objectKey).normalize();
		Path thumbnailFile = rootPath.resolve(thumbnailObjectKey).normalize();
		if (!targetFile.startsWith(rootPath)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid storage target path.");
		}
		if (!thumbnailFile.startsWith(rootPath)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid thumbnail target path.");
		}

		try {
			Files.createDirectories(targetFile.getParent());
			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
			}
			createThumbnail(targetFile, thumbnailFile, resolveFormatName(file));
		}
		catch (IOException exception) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store image.", exception);
		}

		String resourcePath = normalizePublicPath(storageProperties.getPublicPath()) + "/" + objectKey;
		String resourceUrl = buildResourceUrl(resourcePath);
		String thumbnailResourcePath = normalizePublicPath(storageProperties.getPublicPath()) + "/" + thumbnailObjectKey;
		String thumbnailResourceUrl = buildResourceUrl(thumbnailResourcePath);
		return new StoredObject(objectKey, resourceUrl, thumbnailObjectKey, thumbnailResourceUrl, file.getContentType(), file.getSize());
	}

	private void createThumbnail(Path sourceFile, Path thumbnailFile, String formatName) throws IOException {
		BufferedImage sourceImage = ImageIO.read(sourceFile.toFile());
		if (sourceImage == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported image format.");
		}

		BufferedImage thumbnailImage = resizeImage(sourceImage, 320, 320);
		Files.createDirectories(thumbnailFile.getParent());
		if (!ImageIO.write(thumbnailImage, formatName, thumbnailFile.toFile())) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to write thumbnail.");
		}
	}

	private BufferedImage resizeImage(BufferedImage sourceImage, int maxWidth, int maxHeight) {
		int sourceWidth = sourceImage.getWidth();
		int sourceHeight = sourceImage.getHeight();
		if (sourceWidth <= maxWidth && sourceHeight <= maxHeight) {
			return sourceImage;
		}

		double scale = Math.min((double) maxWidth / sourceWidth, (double) maxHeight / sourceHeight);
		int targetWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
		int targetHeight = Math.max(1, (int) Math.round(sourceHeight * scale));
		BufferedImage targetImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = targetImage.createGraphics();
		try {
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
		}
		finally {
			graphics.dispose();
		}
		return targetImage;
	}

	private String buildThumbnailObjectKey(String objectKey, String extension) {
		if (extension.isBlank()) {
			return objectKey + "_thumb";
		}
		return objectKey.substring(0, objectKey.length() - extension.length()) + "_thumb" + extension;
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

	private String resolveFormatName(MultipartFile file) {
		String contentType = file.getContentType();
		if (contentType == null) {
			return "png";
		}
		String subtype = contentType.toLowerCase(Locale.ROOT).substring(contentType.indexOf('/') + 1);
		if ("jpeg".equals(subtype)) {
			return "jpg";
		}
		return subtype;
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

	private String buildResourceUrl(String resourcePath) {
		String publicBaseUrl = storageProperties.getPublicBaseUrl();
		if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
			return resourcePath;
		}

		String normalizedBaseUrl = publicBaseUrl.trim();
		if (normalizedBaseUrl.endsWith("/")) {
			normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
		}
		return normalizedBaseUrl + resourcePath;
	}
}
