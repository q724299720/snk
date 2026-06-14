package com.snk.server.domain.recognition;

public interface ImageRecognitionTaskProvider {

	ImageRecognitionTaskProviderResult recognize(String inputImageUrl);
}
