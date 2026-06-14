package com.snk.server.domain.recognition;

import java.math.BigDecimal;
import java.util.List;

public record ImageRecognitionTaskProviderResult(
	List<String> candidateQueries,
	BigDecimal confidence
) {
}
