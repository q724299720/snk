package com.snk.server.domain.recognition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

final class ServerOcrSearchQueryBuilder {

	private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\\s+");
	private static final Pattern NOISE_PATTERN = Pattern.compile("[^\\p{L}\\p{N}\\s]");

	private ServerOcrSearchQueryBuilder() {
	}

	static List<String> build(String rawText) {
		String trimmedText = rawText == null ? "" : rawText.trim();
		if (trimmedText.isBlank()) {
			return List.of();
		}

		String originalQuery = trimmedText.lines()
			.map(String::trim)
			.filter(line -> !line.isBlank())
			.reduce((left, right) -> left + " " + right)
			.orElse("");

		String compactQuery = LINE_BREAK_PATTERN.matcher(NOISE_PATTERN.matcher(trimmedText).replaceAll(" "))
			.replaceAll(" ")
			.trim();

		String mergedQuery = compactQuery.replace(" ", "");
		Set<String> queries = new LinkedHashSet<>();
		if (!originalQuery.isBlank()) {
			queries.add(originalQuery);
		}
		if (!compactQuery.isBlank()) {
			queries.add(compactQuery);
		}
		if (!mergedQuery.isBlank()) {
			queries.add(mergedQuery);
		}
		return new ArrayList<>(queries);
	}
}
