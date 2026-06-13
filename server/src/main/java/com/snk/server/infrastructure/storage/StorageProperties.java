package com.snk.server.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "snk.storage")
public class StorageProperties {

	private String rootPath = "./runtime/storage";
	private String publicPath = "/uploads";
	private String imageDirectory = "images";
	private long maxImageSizeBytes = 10L * 1024L * 1024L;

	public String getRootPath() {
		return rootPath;
	}

	public void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}

	public String getPublicPath() {
		return publicPath;
	}

	public void setPublicPath(String publicPath) {
		this.publicPath = publicPath;
	}

	public String getImageDirectory() {
		return imageDirectory;
	}

	public void setImageDirectory(String imageDirectory) {
		this.imageDirectory = imageDirectory;
	}

	public long getMaxImageSizeBytes() {
		return maxImageSizeBytes;
	}

	public void setMaxImageSizeBytes(long maxImageSizeBytes) {
		this.maxImageSizeBytes = maxImageSizeBytes;
	}
}
