package com.snk.server.domain.record;

import com.snk.server.infrastructure.persistence.food.FoodItemEntity;
import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import com.snk.server.infrastructure.persistence.record.FoodRecordEntity;
import com.snk.server.infrastructure.persistence.record.FoodRecordImageEntity;
import com.snk.server.infrastructure.persistence.record.FoodRecordImageRepository;
import com.snk.server.infrastructure.persistence.record.FoodRecordRepository;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FoodRecordService {

	private final FoodRecordRepository foodRecordRepository;
	private final FoodRecordImageRepository foodRecordImageRepository;
	private final UserRepository userRepository;
	private final FoodItemRepository foodItemRepository;

	public FoodRecordService(
		FoodRecordRepository foodRecordRepository,
		FoodRecordImageRepository foodRecordImageRepository,
		UserRepository userRepository,
		FoodItemRepository foodItemRepository
	) {
		this.foodRecordRepository = foodRecordRepository;
		this.foodRecordImageRepository = foodRecordImageRepository;
		this.userRepository = userRepository;
		this.foodItemRepository = foodItemRepository;
	}

	@Transactional
	public FoodRecordResult createRecord(FoodRecordCreateCommand command) {
		UserEntity user = userRepository.findById(command.userId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
		FoodItemEntity foodItem = foodItemRepository.findById(command.foodItemId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Food item not found."));

		FoodRecordEntity entity = new FoodRecordEntity();
		entity.setUser(user);
		entity.setFoodItem(foodItem);
		entity.setSourceType(command.sourceType());
		entity.setPublic(command.isPublic());
		entity.setRating(command.rating());
		entity.setComment(command.comment());
		entity.setRecordTime(command.recordTime());

		FoodRecordEntity savedRecord = foodRecordRepository.save(entity);
		List<FoodRecordImageValue> images = normalizeImages(command.images());
		if (!images.isEmpty()) {
			foodRecordImageRepository.saveAll(toImageEntities(savedRecord, images));
		}
		return toResult(savedRecord, images);
	}

	@Transactional
	public FoodRecordResult likeRecord(Long recordId) {
		FoodRecordEntity entity = foodRecordRepository.findById(recordId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Food record not found."));
		entity.setLikeCount(entity.getLikeCount() + 1);
		return toResult(foodRecordRepository.save(entity));
	}

	@Transactional(readOnly = true)
	public List<FoodRecordHistoryItem> listRecentRecords(Long userId, int limit) {
		int normalizedLimit = Math.min(Math.max(limit, 1), 20);
		List<FoodRecordEntity> records = foodRecordRepository.findByUser_IdAndDeletedAtIsNullOrderByRecordTimeDesc(
			userId,
			PageRequest.of(0, normalizedLimit)
		);
		Map<Long, List<FoodRecordImageValue>> imagesByRecordId = imagesByRecordId(
			records.stream()
				.map(FoodRecordEntity::getId)
				.toList()
		);
		return records.stream()
			.map(entity -> toHistoryItem(entity, imagesByRecordId.getOrDefault(entity.getId(), List.of())))
			.toList();
	}

	private FoodRecordResult toResult(FoodRecordEntity entity) {
		return toResult(entity, imagesForRecord(entity.getId()));
	}

	private FoodRecordResult toResult(FoodRecordEntity entity, List<FoodRecordImageValue> images) {
		return new FoodRecordResult(
			entity.getId(),
			entity.getUser().getId(),
			entity.getFoodItem().getId(),
			entity.getSourceType(),
			entity.isPublic(),
			entity.getRating(),
			entity.getComment(),
			entity.getLikeCount(),
			entity.getRecordTime(),
			entity.getCreatedAt(),
			images
		);
	}

	private FoodRecordHistoryItem toHistoryItem(FoodRecordEntity entity, List<FoodRecordImageValue> images) {
		return new FoodRecordHistoryItem(
			entity.getId(),
			entity.getUser().getId(),
			entity.getFoodItem().getId(),
			entity.getFoodItem().getName(),
			entity.getFoodItem().getItemType(),
			entity.getFoodItem().getCategory(),
			entity.getFoodItem().getSubcategory(),
			entity.getFoodItem().getBrand(),
			entity.getFoodItem().getCoverImageUrl(),
			entity.getSourceType(),
			entity.isPublic(),
			entity.getRating(),
			entity.getComment(),
			entity.getLikeCount(),
			entity.getRecordTime(),
			entity.getCreatedAt(),
			images
		);
	}

	private List<FoodRecordImageEntity> toImageEntities(
		FoodRecordEntity record,
		List<FoodRecordImageValue> images
	) {
		return images.stream()
			.map(image -> {
				FoodRecordImageEntity entity = new FoodRecordImageEntity();
				entity.setRecord(record);
				entity.setImageUrl(image.imageUrl());
				entity.setThumbnailUrl(image.thumbnailUrl());
				return entity;
			})
			.toList();
	}

	private Map<Long, List<FoodRecordImageValue>> imagesByRecordId(Collection<Long> recordIds) {
		if (recordIds.isEmpty()) {
			return Map.of();
		}
		List<FoodRecordImageEntity> entities = foodRecordImageRepository.findByRecord_IdInOrderByCreatedAtAsc(recordIds);
		if (entities == null) {
			return Map.of();
		}
		return entities.stream()
			.collect(
				Collectors.groupingBy(
					entity -> entity.getRecord().getId(),
					Collectors.mapping(this::toImageValue, Collectors.toList())
				)
			);
	}

	private List<FoodRecordImageValue> imagesForRecord(Long recordId) {
		return imagesByRecordId(List.of(recordId)).getOrDefault(recordId, List.of());
	}

	private List<FoodRecordImageValue> normalizeImages(List<FoodRecordImageValue> images) {
		if (images == null) {
			return List.of();
		}
		return images.stream()
			.filter(image -> image.imageUrl() != null && !image.imageUrl().isBlank())
			.toList();
	}

	private FoodRecordImageValue toImageValue(FoodRecordImageEntity entity) {
		return new FoodRecordImageValue(entity.getImageUrl(), entity.getThumbnailUrl());
	}
}
