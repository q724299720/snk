package com.snk.server.infrastructure.recognition;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "snk.recognition")
public class RecognitionProperties {

	private final Ocr ocr = new Ocr();
	private final Image image = new Image();

	public Ocr getOcr() {
		return ocr;
	}

	public Image getImage() {
		return image;
	}

	public static class Ocr {

		private String provider = "disabled";
		private String stubText;

		public String getProvider() {
			return provider;
		}

		public void setProvider(String provider) {
			this.provider = provider;
		}

		public String getStubText() {
			return stubText;
		}

		public void setStubText(String stubText) {
			this.stubText = stubText;
		}
	}

	public static class Image {

		private String provider = "disabled";
		private String stubQuery;

		public String getProvider() {
			return provider;
		}

		public void setProvider(String provider) {
			this.provider = provider;
		}

		public String getStubQuery() {
			return stubQuery;
		}

		public void setStubQuery(String stubQuery) {
			this.stubQuery = stubQuery;
		}
	}
}
