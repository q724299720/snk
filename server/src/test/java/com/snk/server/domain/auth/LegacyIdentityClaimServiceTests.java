package com.snk.server.domain.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.snk.server.infrastructure.persistence.auth.LegacyIdentityClaimRepository;
import com.snk.server.infrastructure.persistence.food.FoodItemReportRepository;
import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import com.snk.server.infrastructure.persistence.record.FoodRecordCommentRepository;
import com.snk.server.infrastructure.persistence.record.FoodRecordRepository;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LegacyIdentityClaimServiceTests {
	@Mock UserRepository users;
	@Mock LegacyIdentityClaimRepository claims;
	@Mock FoodRecordRepository records;
	@Mock FoodRecordCommentRepository comments;
	@Mock FoodItemReportRepository reports;
	@Mock FoodItemRepository foods;

	@Test
	void migratesEveryLegacyOwnershipReference() {
		UserEntity legacy = new UserEntity(); UserEntity account = new UserEntity();
		when(users.findByAnonymousInstallationIdForUpdate("old-install")).thenReturn(Optional.of(legacy));
		when(users.findById(9L)).thenReturn(Optional.of(account));
		LegacyIdentityClaimService service = new LegacyIdentityClaimService(users, claims, records, comments, reports, foods);

		service.claim(9L, "old-install");

		verify(records).reassignUser(legacy, account);
		verify(comments).reassignUser(legacy, account);
		verify(reports).reassignReporter(legacy, account);
		verify(foods).reassignCreator(legacy, account);
		verify(claims).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void rejectsRepeatedClaim() {
		when(claims.existsByInstallationId("old-install")).thenReturn(true);
		LegacyIdentityClaimService service = new LegacyIdentityClaimService(users, claims, records, comments, reports, foods);
		assertThatThrownBy(() -> service.claim(9L, "old-install"))
			.isInstanceOf(AuthException.class).hasMessage("LEGACY_IDENTITY_ALREADY_CLAIMED");
	}
}
