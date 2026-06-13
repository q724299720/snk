package com.snk.server.infrastructure.storage;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StorageWebMvcConfigurer implements WebMvcConfigurer {

	private final StorageProperties storageProperties;

	public StorageWebMvcConfigurer(StorageProperties storageProperties) {
		this.storageProperties = storageProperties;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String publicPath = normalizePublicPath(storageProperties.getPublicPath()) + "/**";
		String location = Path.of(storageProperties.getRootPath()).toAbsolutePath().normalize().toUri().toString();
		registry.addResourceHandler(publicPath).addResourceLocations(location);
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
