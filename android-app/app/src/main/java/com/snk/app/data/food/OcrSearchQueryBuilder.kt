package com.snk.app.data.food

internal object OcrSearchQueryBuilder {
    private val lineBreakRegex = Regex("\\s+")
    private val noiseRegex = Regex("[^\\p{L}\\p{N}\\s]")

    fun build(rawText: String): List<String> {
        val trimmedText = rawText.trim()
        if (trimmedText.isBlank()) {
            return emptyList()
        }

        val originalQuery = trimmedText
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .joinToString(" ")

        val compactQuery = trimmedText
            .replace(noiseRegex, " ")
            .replace(lineBreakRegex, " ")
            .trim()

        val mergedQuery = compactQuery.replace(" ", "")

        return linkedSetOf(originalQuery, compactQuery, mergedQuery)
            .filter(String::isNotBlank)
    }
}
