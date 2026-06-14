package com.snk.server.infrastructure.recognition;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "snk.recognition")
public class RecognitionProperties {

	private final Ocr ocr = new Ocr();

	public Ocr getOcr() {
		return ocr;
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
}
