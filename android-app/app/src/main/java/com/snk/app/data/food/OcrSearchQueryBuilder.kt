package com.snk.app.data.food

internal object OcrSearchQueryBuilder {
    private val lineBreakRegex = Regex("\\s+")
    private val noiseRegex = Regex("[^\\p{L}\\p{N}\\s]")
    private val chineseRegex = Regex("\\p{IsHan}")
    private val foodSuffixKeywords = listOf(
        "牛肉面",
        "方便面",
        "泡面",
        "拉面",
        "拌面",
        "薯片",
        "饼干",
        "蛋糕",
        "面包",
        "汉堡",
        "奶茶",
        "咖啡",
        "酸奶",
        "果汁",
        "可乐",
        "汽水",
    )

    fun build(rawText: String): List<String> {
        val normalized = normalize(rawText) ?: return emptyList()
        return linkedSetOf<String>().apply {
            add(normalized.originalQuery)
            add(normalized.compactQuery)
            add(normalized.mergedQuery)
            addAll(buildChinesePhraseQueries(normalized.mergedQuery))
            addAll(buildTokenQueries(normalized.compactQuery))
        }.filter(String::isNotBlank)
    }

    fun buildDisplayQueries(rawText: String): List<String> {
        val normalized = normalize(rawText) ?: return emptyList()
        return linkedSetOf<String>().apply {
            add(normalized.compactQuery)
            addAll(buildChinesePhraseQueries(normalized.mergedQuery))
            addAll(buildTokenQueries(normalized.compactQuery))
        }.filter(String::isNotBlank)
    }

    private fun normalize(rawText: String): NormalizedQuery? {
        val trimmedText = rawText.trim()
        if (trimmedText.isBlank()) {
            return null
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

        if (compactQuery.isBlank()) {
            return null
        }

        return NormalizedQuery(
            originalQuery = originalQuery,
            compactQuery = compactQuery,
            mergedQuery = compactQuery.replace(" ", ""),
        )
    }

    private fun buildChinesePhraseQueries(mergedQuery: String): List<String> {
        if (mergedQuery.isBlank() || !chineseRegex.containsMatchIn(mergedQuery)) {
            return emptyList()
        }

        val result = linkedSetOf<String>()
        result += mergedQuery

        val matchedKeyword = foodSuffixKeywords.firstOrNull { keyword ->
            mergedQuery.contains(keyword)
        } ?: return result.toList()

        val keywordStart = mergedQuery.indexOf(matchedKeyword)
        val descriptorStart = (keywordStart - 2).coerceAtLeast(0)
        val descriptorQuery = mergedQuery.substring(descriptorStart)
        val brandQuery = mergedQuery.substring(0, descriptorStart)

        result += descriptorQuery
        result += matchedKeyword
        if (brandQuery.length >= 2) {
            result += brandQuery
        }
        return result.toList()
    }

    private fun buildTokenQueries(compactQuery: String): List<String> {
        val tokens = compactQuery.split(" ")
            .map(String::trim)
            .filter(String::isNotBlank)
        if (tokens.size < 2) {
            return emptyList()
        }

        val result = linkedSetOf<String>()
        if (tokens.size >= 2) {
            result += tokens.takeLast(2).joinToString(" ")
        }
        result += tokens.last()
        result += tokens.first()
        return result.toList()
    }

    private data class NormalizedQuery(
        val originalQuery: String,
        val compactQuery: String,
        val mergedQuery: String,
    )
}
