package com.snk.server.domain.record;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.snk.server.infrastructure.persistence.food.FoodItemEntity;
import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import com.snk.server.infrastructure.persistence.record.FoodRecordEntity;
import com.snk.server.infrastructure.persistence.record.FoodRecordCommentEntity;
import com.snk.server.infrastructure.persistence.record.FoodRecordCommentRepository;
import com.snk.server.infrastructure.persistence.record.FoodRecordImageEntity;
import com.snk.server.infrastructure.persistence.record.FoodRecordImageRepository;
import com.snk.server.infrastructure.persistence.record.FoodRecordRepository;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class FoodRecordServiceTests {

	@Mock
	private FoodRecordRepository foodRecordRepository;

	@Mock
	private FoodRecordImageRepository foodRecordImageRepository;

	@Mock
	private FoodRecordCommentRepository foodRecordCommentRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private FoodItemRepository foodItemRepository;

	@InjectMocks
	private FoodRecordService foodRecordService;

	@Test
	void shouldCreateFoodRecordWithImages() throws Exception {
		UserEntity user = new UserEntity();
		setUserId(user, 100L);

		FoodItemEntity foodItem = new FoodItemEntity();
		setFoodItemId(foodItem, 200L);

		when(userRepository.findById(100L)).thenReturn(Optional.of(user));
		when(foodItemRepository.findById(200L)).thenReturn(Optional.of(foodItem));
		when(foodRecordRepository.save(any(FoodRecordEntity.class))).thenAnswer(invocation -> {
			FoodRecordEntity entity = invocation.getArgument(0);
			setRecordId(entity, 1L);
			setCreatedAt(entity, OffsetDateTime.parse("2026-06-13T23:30:00Z"));
			if (entity.getRecordTime() == null) {
				entity.setRecordTime(OffsetDateTime.parse("2026-06-13T23:30:00Z"));
			}
			return entity;
		});

		FoodRecordResult result = foodRecordService.createRecord(
			new FoodRecordCreateCommand(
				100L,
				200L,
				"text_search",
				false,
				(short) 4,
				"tasty",
				OffsetDateTime.parse("2026-06-13T23:29:00Z"),
				List.of(
					new FoodRecordImageValue(
						"https://snk.qiuxinmin.cn/uploads/records/noodle.jpg",
						"https://snk.qiuxinmin.cn/uploads/records/noodle-thumb.jpg"
					)
				)
			)
		);

		ArgumentCaptor<FoodRecordEntity> captor = ArgumentCaptor.forClass(FoodRecordEntity.class);
		verify(foodRecordRepository).save(captor.capture());
		FoodRecordEntity saved = captor.getValue();
		assertThat(saved.getUser().getId()).isEqualTo(100L);
		assertThat(saved.getFoodItem().getId()).isEqualTo(200L);
		assertThat(saved.getSourceType()).isEqualTo("text_search");
		assertThat(saved.getRating()).isEqualTo((short) 4);
		assertThat(result.likeCount()).isZero();
		assertThat(result.id()).isEqualTo(1L);
		assertThat(result.foodItemId()).isEqualTo(200L);
		assertThat(result.images()).hasSize(1);
		assertThat(result.images().getFirst().thumbnailUrl())
			.isEqualTo("https://snk.qiuxinmin.cn/uploads/records/noodle-thumb.jpg");

		ArgumentCaptor<List<FoodRecordImageEntity>> imageCaptor = ArgumentCaptor.forClass(List.class);
		verify(foodRecordImageRepository).saveAll(imageCaptor.capture());
		assertThat(imageCaptor.getValue()).hasSize(1);
		assertThat(imageCaptor.getValue().getFirst().getRecord().getId()).isEqualTo(1L);
		assertThat(imageCaptor.getValue().getFirst().getImageUrl())
			.isEqualTo("https://snk.qiuxinmin.cn/uploads/records/noodle.jpg");
	}

	@Test
	void shouldIncreaseLikeCount() throws Exception {
		UserEntity user = new UserEntity();
		setUserId(user, 100L);

		FoodItemEntity foodItem = new FoodItemEntity();
		setFoodItemId(foodItem, 200L);

		FoodRecordEntity record = createRecordEntity(user, foodItem);
		record.setLikeCount(2);

		when(foodRecordRepository.findById(1L)).thenReturn(Optional.of(record));
		when(foodRecordRepository.save(any(FoodRecordEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		FoodRecordResult result = foodRecordService.likeRecord(1L);

		assertThat(result.likeCount()).isEqualTo(3);
		verify(foodRecordRepository).save(record);
	}

	@Test
	void shouldListRecentRecordsWithImages() throws Exception {
		UserEntity user = new UserEntity();
		setUserId(user, 100L);

		FoodItemEntity foodItem = new FoodItemEntity();
		setFoodItemId(foodItem, 200L);
		foodItem.setName("Lays Cucumber Chips");
		foodItem.setItemType("packaged_product");
		foodItem.setCategory("snack");
		foodItem.setSubcategory("chips");
		foodItem.setBrand("Lays");
		foodItem.setCoverImageUrl("https://snk.qiuxinmin.cn/images/1.png");

		FoodRecordEntity record = createRecordEntity(user, foodItem);
		record.setRating((short) 5);
		record.setComment("tasty");
		record.setLikeCount(2);

		when(foodRecordRepository.findByUser_IdAndDeletedAtIsNullOrderByRecordTimeDesc(eq(100L), any()))
			.thenReturn(List.of(record));
		FoodRecordImageEntity image = new FoodRecordImageEntity();
		image.setRecord(record);
		image.setImageUrl("https://snk.qiuxinmin.cn/uploads/records/chips.jpg");
		image.setThumbnailUrl("https://snk.qiuxinmin.cn/uploads/records/chips-thumb.jpg");
		when(foodRecordImageRepository.findByRecord_IdInOrderByCreatedAtAsc(List.of(1L)))
			.thenReturn(List.of(image));

		List<FoodRecordHistoryItem> result = foodRecordService.listRecentRecords(100L, 10);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().foodName()).isEqualTo("Lays Cucumber Chips");
		assertThat(result.getFirst().foodItemType()).isEqualTo("packaged_product");
		assertThat(result.getFirst().foodCoverImageUrl()).isEqualTo("https://snk.qiuxinmin.cn/images/1.png");
		assertThat(result.getFirst().rating()).isEqualTo((short) 5);
		assertThat(result.getFirst().images()).hasSize(1);
		assertThat(result.getFirst().images().getFirst().imageUrl())
			.isEqualTo("https://snk.qiuxinmin.cn/uploads/records/chips.jpg");
	}

	@Test
	void shouldListPublicRecordsWithImages() throws Exception {
		UserEntity user = new UserEntity();
		setUserId(user, 101L);

		FoodItemEntity foodItem = new FoodItemEntity();
		setFoodItemId(foodItem, 201L);
		foodItem.setName("Kangshifu Beef Noodles");
		foodItem.setItemType("packaged_product");
		foodItem.setCategory("instant_food");
		foodItem.setSubcategory("noodles");
		foodItem.setBrand("Kangshifu");
		foodItem.setCoverImageUrl("https://snk.qiuxinmin.cn/images/noodle.png");

		FoodRecordEntity record = createRecordEntity(user, foodItem);
		record.setPublic(true);
		record.setRating((short) 4);
		record.setComment("share this");
		record.setLikeCount(7);

		when(foodRecordRepository.findByIsPublicTrueAndDeletedAtIsNullOrderByRecordTimeDesc(any()))
			.thenReturn(List.of(record));
		FoodRecordImageEntity image = new FoodRecordImageEntity();
		image.setRecord(record);
		image.setImageUrl("https://snk.qiuxinmin.cn/uploads/records/noodle.jpg");
		image.setThumbnailUrl("https://snk.qiuxinmin.cn/uploads/records/noodle-thumb.jpg");
		when(foodRecordImageRepository.findByRecord_IdInOrderByCreatedAtAsc(List.of(1L)))
			.thenReturn(List.of(image));

		List<FoodRecordHistoryItem> result = foodRecordService.listPublicRecords(10);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().isPublic()).isTrue();
		assertThat(result.getFirst().foodName()).isEqualTo("Kangshifu Beef Noodles");
		assertThat(result.getFirst().likeCount()).isEqualTo(7);
		assertThat(result.getFirst().images()).hasSize(1);
		assertThat(result.getFirst().images().getFirst().thumbnailUrl())
			.isEqualTo("https://snk.qiuxinmin.cn/uploads/records/noodle-thumb.jpg");
		verify(foodRecordRepository).findByIsPublicTrueAndDeletedAtIsNullOrderByRecordTimeDesc(any());
	}

	@Test
	void shouldCreateCommentForPublicRecord() throws Exception {
		UserEntity user = new UserEntity();
		setUserId(user, 100L);

		FoodItemEntity foodItem = new FoodItemEntity();
		setFoodItemId(foodItem, 200L);

		FoodRecordEntity record = createRecordEntity(user, foodItem);
		record.setPublic(true);

		when(foodRecordRepository.findById(1L)).thenReturn(Optional.of(record));
		when(userRepository.findById(100L)).thenReturn(Optional.of(user));
		when(foodRecordCommentRepository.save(any(FoodRecordCommentEntity.class))).thenAnswer(invocation -> {
			FoodRecordCommentEntity entity = invocation.getArgument(0);
			setCommentId(entity, 9L);
			setCommentCreatedAt(entity, OffsetDateTime.parse("2026-06-21T12:00:00Z"));
			return entity;
		});

		FoodRecordCommentResult result = foodRecordService.createComment(1L, 100L, "  看起来不错  ");

		assertThat(result.id()).isEqualTo(9L);
		assertThat(result.recordId()).isEqualTo(1L);
		assertThat(result.userId()).isEqualTo(100L);
		assertThat(result.content()).isEqualTo("看起来不错");

		ArgumentCaptor<FoodRecordCommentEntity> captor = ArgumentCaptor.forClass(FoodRecordCommentEntity.class);
		verify(foodRecordCommentRepository).save(captor.capture());
		assertThat(captor.getValue().getRecord().getId()).isEqualTo(1L);
		assertThat(captor.getValue().getUser().getId()).isEqualTo(100L);
		assertThat(captor.getValue().getContent()).isEqualTo("看起来不错");
	}

	@Test
	void shouldRejectCommentForPrivateRecord() throws Exception {
		UserEntity user = new UserEntity();
		setUserId(user, 100L);

		FoodItemEntity foodItem = new FoodItemEntity();
		setFoodItemId(foodItem, 200L);

		FoodRecordEntity record = createRecordEntity(user, foodItem);
		record.setPublic(false);

		when(foodRecordRepository.findById(1L)).thenReturn(Optional.of(record));

		try {
			foodRecordService.createComment(1L, 100L, "看起来不错");
		} catch (ResponseStatusException exception) {
			assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
			return;
		}

		throw new AssertionError("Expected private record comment to be rejected.");
	}

	@Test
	void shouldListCommentsForPublicRecord() throws Exception {
		UserEntity user = new UserEntity();
		setUserId(user, 100L);

		FoodItemEntity foodItem = new FoodItemEntity();
		setFoodItemId(foodItem, 200L);

		FoodRecordEntity record = createRecordEntity(user, foodItem);
		record.setPublic(true);

		FoodRecordCommentEntity comment = new FoodRecordCommentEntity();
		setCommentId(comment, 9L);
		setCommentCreatedAt(comment, OffsetDateTime.parse("2026-06-21T12:00:00Z"));
		comment.setRecord(record);
		comment.setUser(user);
		comment.setContent("看起来不错");

		when(foodRecordRepository.findById(1L)).thenReturn(Optional.of(record));
		when(foodRecordCommentRepository.findByRecord_IdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
			.thenReturn(List.of(comment));

		List<FoodRecordCommentResult> result = foodRecordService.listComments(1L, 10);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().id()).isEqualTo(9L);
		assertThat(result.getFirst().content()).isEqualTo("看起来不错");
	}

	@Test
	void shouldGetOwnRecordWithImages() throws Exception {
		UserEntity user = new UserEntity();
		setUserId(user, 100L);

		FoodItemEntity foodItem = new FoodItemEntity();
		setFoodItemId(foodItem, 200L);

		FoodRecordEntity record = createRecordEntity(user, foodItem);
		record.setRating((short) 4);
		record.setComment("old comment");

		FoodRecordImageEntity image = new FoodRecordImageEntity();
		image.setRecord(record);
		image.setImageUrl("https://snk.qiuxinmin.cn/uploads/records/noodle.jpg");
		image.setThumbnailUrl("https://snk.qiuxinmin.cn/uploads/records/noodle-thumb.jpg");

		when(foodRecordRepository.findById(1L)).thenReturn(Optional.of(record));
		when(foodRecordImageRepository.findByRecord_IdInOrderByCreatedAtAsc(List.of(1L)))
			.thenReturn(List.of(image));

		FoodRecordResult result = foodRecordService.getRecordForUser(1L, 100L);

		assertThat(result.id()).isEqualTo(1L);
		assertThat(result.userId()).isEqualTo(100L);
		assertThat(result.rating()).isEqualTo((short) 4);
		assertThat(result.comment()).isEqualTo("old comment");
		assertThat(result.images()).hasSize(1);
		assertThat(result.images().getFirst().thumbnailUrl())
			.isEqualTo("https://snk.qiuxinmin.cn/uploads/records/noodle-thumb.jpg");
	}

	@Test
	void shouldUpdateOwnRecordAndKeepImages() throws Exception {
		UserEntity user = new UserEntity();
		setUserId(user, 100L);

		FoodItemEntity foodItem = new FoodItemEntity();
		setFoodItemId(foodItem, 200L);

		FoodRecordEntity record = createRecordEntity(user, foodItem);
		record.setRating((short) 3);
		record.setComment("old comment");
		record.setPublic(false);

		FoodRecordImageEntity image = new FoodRecordImageEntity();
		image.setRecord(record);
		image.setImageUrl("https://snk.qiuxinmin.cn/uploads/records/noodle.jpg");
		image.setThumbnailUrl("https://snk.qiuxinmin.cn/uploads/records/noodle-thumb.jpg");

		when(foodRecordRepository.findById(1L)).thenReturn(Optional.of(record));
		when(foodRecordRepository.save(record)).thenReturn(record);
		when(foodRecordImageRepository.findByRecord_IdInOrderByCreatedAtAsc(List.of(1L)))
			.thenReturn(List.of(image));

		FoodRecordResult result = foodRecordService.updateRecord(
			new FoodRecordUpdateCommand(
				1L,
				100L,
				(short) 5,
				"better after edit",
				true
			)
		);

		assertThat(record.getRating()).isEqualTo((short) 5);
		assertThat(record.getComment()).isEqualTo("better after edit");
		assertThat(record.isPublic()).isTrue();
		assertThat(result.rating()).isEqualTo((short) 5);
		assertThat(result.comment()).isEqualTo("better after edit");
		assertThat(result.isPublic()).isTrue();
		assertThat(result.images()).hasSize(1);
		verify(foodRecordRepository).save(record);
	}

	@Test
	void shouldRejectUpdateWhenRecordBelongsToAnotherUser() throws Exception {
		UserEntity owner = new UserEntity();
		setUserId(owner, 100L);

		FoodItemEntity foodItem = new FoodItemEntity();
		setFoodItemId(foodItem, 200L);

		FoodRecordEntity record = createRecordEntity(owner, foodItem);

		when(foodRecordRepository.findById(1L)).thenReturn(Optional.of(record));

		try {
			foodRecordService.updateRecord(
				new FoodRecordUpdateCommand(
					1L,
					999L,
					(short) 5,
					"not mine",
					true
				)
			);
		} catch (ResponseStatusException exception) {
			assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
			return;
		}

		throw new AssertionError("Expected editing another user's record to be rejected.");
	}

	private FoodRecordEntity createRecordEntity(UserEntity user, FoodItemEntity foodItem) throws Exception {
		FoodRecordEntity record = new FoodRecordEntity();
		setRecordId(record, 1L);
		setCreatedAt(record, OffsetDateTime.parse("2026-06-13T23:30:00Z"));
		record.setUser(user);
		record.setFoodItem(foodItem);
		record.setSourceType("text_search");
		record.setPublic(false);
		record.setRating((short) 4);
		record.setComment("good");
		record.setRecordTime(OffsetDateTime.parse("2026-06-13T23:29:00Z"));
		return record;
	}

	private void setUserId(UserEntity entity, Long id) throws Exception {
		Field field = UserEntity.class.getDeclaredField("id");
		field.setAccessible(true);
		field.set(entity, id);
	}

	private void setFoodItemId(FoodItemEntity entity, Long id) throws Exception {
		Field field = FoodItemEntity.class.getDeclaredField("id");
		field.setAccessible(true);
		field.set(entity, id);
	}

	private void setRecordId(FoodRecordEntity entity, Long id) throws Exception {
		Field field = FoodRecordEntity.class.getDeclaredField("id");
		field.setAccessible(true);
		field.set(entity, id);
	}

	private void setCreatedAt(FoodRecordEntity entity, OffsetDateTime value) throws Exception {
		Field field = FoodRecordEntity.class.getDeclaredField("createdAt");
		field.setAccessible(true);
		field.set(entity, value);
	}

	private void setCommentId(FoodRecordCommentEntity entity, Long id) throws Exception {
		Field field = FoodRecordCommentEntity.class.getDeclaredField("id");
		field.setAccessible(true);
		field.set(entity, id);
	}

	private void setCommentCreatedAt(FoodRecordCommentEntity entity, OffsetDateTime value) throws Exception {
		Field field = FoodRecordCommentEntity.class.getDeclaredField("createdAt");
		field.setAccessible(true);
		field.set(entity, value);
	}
}
