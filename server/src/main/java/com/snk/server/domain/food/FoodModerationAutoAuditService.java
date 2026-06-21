package com.snk.server.domain.food;

import com.snk.server.infrastructure.moderation.ModerationProperties;
import com.snk.server.infrastructure.persistence.food.FoodItemEntity;
import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import com.snk.server.infrastructure.persistence.review.ReviewConfigWordEntity;
import com.snk.server.infrastructure.persistence.review.ReviewConfigWordRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class FoodModerationAutoAuditService {

	private static final String VALID_FOOD_WORD_TYPE = "valid_food_word";
	private static final int EXTREMELY_SHORT_GARBAGE_MAX_LENGTH = 2;
	private static final Pattern MEANINGFUL_CHARACTER_PATTERN = Pattern.compile("[\\p{IsHan}A-Za-z0-9]");
	private static final Pattern HAN_CHARACTER_PATTERN = Pattern.compile("\\p{IsHan}");

	private final FoodItemRepository foodItemRepository;
	private final FoodModerationService foodModerationService;
	private final ReviewConfigWordRepository reviewConfigWordRepository;
	private final ModerationProperties moderationProperties;
	private final Clock clock;

	public FoodModerationAutoAuditService(
		FoodItemRepository foodItemRepository,
		FoodModerationService foodModerationService,
		ReviewConfigWordRepository reviewConfigWordRepository,
		ModerationProperties moderationProperties,
		Clock clock
	) {
		this.foodItemRepository = foodItemRepository;
		this.foodModerationService = foodModerationService;
		this.reviewConfigWordRepository = reviewConfigWordRepository;
		this.moderationProperties = moderationProperties;
		this.clock = clock;
	}

	@Scheduled(
		fixedDelayString = "${snk.moderation.auto-audit.fixed-delay-ms:3600000}",
		initialDelayString = "${snk.moderation.auto-audit.initial-delay-ms:300000}"
	)
	public void scheduledAutoAudit() {
		runAutoAudit();
	}

	public AutoAuditSummary runAutoAudit() {
		ModerationProperties.AutoAudit autoAudit = moderationProperties.getAutoAudit();
		if (!autoAudit.isEnabled()) {
			return new AutoAuditSummary(0, 0, 0, null, List.of());
		}

		OffsetDateTime cutoffAt = OffsetDateTime.now(clock).minusHours(Math.max(autoAudit.getPendingAgeHours(), 1));
		List<FoodItemEntity> candidates = foodItemRepository.findByAuditStatusAndCreatedAtBeforeOrderByCreatedAtAsc("pending", cutoffAt);
		if (candidates.isEmpty()) {
			return new AutoAuditSummary(0, 0, 0, cutoffAt, List.of());
		}
		List<String> validFoodWords = loadEnabledValidFoodWords();
		List<Long> rejectedIds = new ArrayList<>();

		for (FoodItemEntity candidate : candidates) {
			if (shouldRejectAutomatically(candidate, autoAudit.getRejectKeywords(), validFoodWords)) {
				foodModerationService.rejectFoodItem(candidate.getId());
				rejectedIds.add(candidate.getId());
			}
		}

		return new AutoAuditSummary(
			candidates.size(),
			rejectedIds.size(),
			candidates.size() - rejectedIds.size(),
			cutoffAt,
			List.copyOf(rejectedIds)
		);
	}

	private List<String> loadEnabledValidFoodWords() {
		return reviewConfigWordRepository.findByEnabledAndWordTypeOrderByUpdatedAtDesc(true, VALID_FOOD_WORD_TYPE)
			.stream()
			.map(ReviewConfigWordEntity::getWord)
			.toList();
	}

	private boolean shouldRejectAutomatically(FoodItemEntity item, List<String> rejectKeywords, List<String> validFoodWords) {
		String normalizedName = normalize(item.getName());
		if (normalizedName == null) {
			return true;
		}
		if (!containsMeaningfulCharacter(normalizedName)) {
			return true;
		}
		if (looksLikeRepeatedGarbage(normalizedName)) {
			return true;
		}
		if (looksLikeExtremelyShortGarbage(normalizedName, validFoodWords)) {
			return true;
		}
		if (containsRejectKeyword(normalizedName, rejectKeywords)) {
			return true;
		}
		return false;
	}

	private boolean containsMeaningfulCharacter(String value) {
		return MEANINGFUL_CHARACTER_PATTERN.matcher(value).find();
	}

	private boolean looksLikeRepeatedGarbage(String value) {
		if (value.length() < 3) {
			return false;
		}
		char first = value.charAt(0);
		for (int index = 1; index < value.length(); index++) {
			if (value.charAt(index) != first) {
				return false;
			}
		}
		return true;
	}

	private boolean looksLikeExtremelyShortGarbage(String value, List<String> validFoodWords) {
		return value.length() <= EXTREMELY_SHORT_GARBAGE_MAX_LENGTH
			&& !containsHanCharacter(value)
			&& !containsValidFoodWord(value, validFoodWords);
	}

	private boolean containsHanCharacter(String value) {
		return HAN_CHARACTER_PATTERN.matcher(value).find();
	}

	private boolean containsValidFoodWord(String normalizedName, List<String> validFoodWords) {
		if (validFoodWords == null || validFoodWords.isEmpty()) {
			return false;
		}
		String lowerCaseName = normalizedName.toLowerCase(Locale.ROOT);
		for (String validWord : validFoodWords) {
			String normalizedValidWord = normalize(validWord);
			if (normalizedValidWord == null) {
				continue;
			}
			if (lowerCaseName.contains(normalizedValidWord.toLowerCase(Locale.ROOT))) {
				return true;
			}
		}
		return false;
	}

	private boolean containsRejectKeyword(String normalizedName, List<String> rejectKeywords) {
		if (rejectKeywords == null || rejectKeywords.isEmpty()) {
			return false;
		}
		String lowerCaseName = normalizedName.toLowerCase(Locale.ROOT);
		for (String keyword : rejectKeywords) {
			String normalizedKeyword = normalize(keyword);
			if (normalizedKeyword == null) {
				continue;
			}
			String lowerCaseKeyword = normalizedKeyword.toLowerCase(Locale.ROOT);
			if (lowerCaseName.contains(lowerCaseKeyword)) {
				return true;
			}
		}
		return false;
	}

	private String normalize(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim().replaceAll("\\s+", " ");
		return normalized.isBlank() ? null : normalized;
	}

	public record AutoAuditSummary(
		int scannedCount,
		int rejectedCount,
		int keptPendingCount,
		OffsetDateTime cutoffAt,
		List<Long> rejectedFoodItemIds
	) {
	}
}
