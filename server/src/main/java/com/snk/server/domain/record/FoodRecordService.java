package com.snk.server.domain.record;

import com.snk.server.infrastructure.persistence.food.FoodItemEntity;
import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import com.snk.server.infrastructure.persistence.record.FoodRecordEntity;
import com.snk.server.infrastructure.persistence.record.FoodRecordRepository;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FoodRecordService {

	private final FoodRecordRepository foodRecordRepository;
	private final UserRepository userRepository;
	private final FoodItemRepository foodItemRepository;

	public FoodRecordService(
		FoodRecordRepository foodRecordRepository,
		UserRepository userRepository,
		FoodItemRepository foodItemRepository
	) {
		this.foodRecordRepository = foodRecordRepository;
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

		return toResult(foodRecordRepository.save(entity));
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
		return foodRecordRepository.findByUser_IdAndDeletedAtIsNullOrderByRecordTimeDesc(
			userId,
			PageRequest.of(0, normalizedLimit)
		).stream()
			.map(this::toHistoryItem)
			.toList();
	}

	private FoodRecordResult toResult(FoodRecordEntity entity) {
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
			entity.getCreatedAt()
		);
	}

	private FoodRecordHistoryItem toHistoryItem(FoodRecordEntity entity) {
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
			entity.getCreatedAt()
		);
	}
}
