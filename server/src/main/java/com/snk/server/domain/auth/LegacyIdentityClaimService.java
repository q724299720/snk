package com.snk.server.domain.auth;

import com.snk.server.infrastructure.persistence.auth.LegacyIdentityClaimEntity;
import com.snk.server.infrastructure.persistence.auth.LegacyIdentityClaimRepository;
import com.snk.server.infrastructure.persistence.food.FoodItemReportRepository;
import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import com.snk.server.infrastructure.persistence.record.FoodRecordCommentRepository;
import com.snk.server.infrastructure.persistence.record.FoodRecordRepository;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LegacyIdentityClaimService {
	private final UserRepository users; private final LegacyIdentityClaimRepository claims;
	private final FoodRecordRepository records; private final FoodRecordCommentRepository comments;
	private final FoodItemReportRepository reports; private final FoodItemRepository foods;

	public LegacyIdentityClaimService(UserRepository users, LegacyIdentityClaimRepository claims,
		FoodRecordRepository records, FoodRecordCommentRepository comments,
		FoodItemReportRepository reports, FoodItemRepository foods) {
		this.users=users; this.claims=claims; this.records=records; this.comments=comments; this.reports=reports; this.foods=foods;
	}

	@Transactional
	public void claim(Long accountId, String installationId) {
		if (claims.existsByInstallationId(installationId)) throw conflict();
		UserEntity legacy = users.findByAnonymousInstallationIdForUpdate(installationId).orElseThrow(LegacyIdentityClaimService::conflict);
		UserEntity account = users.findById(accountId).orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED"));
		if (legacy.getClaimedByUser() != null || legacy == account) throw conflict();
		records.reassignUser(legacy, account); comments.reassignUser(legacy, account);
		reports.reassignReporter(legacy, account); foods.reassignCreator(legacy, account);
		legacy.setClaimedByUser(account); legacy.setClaimedAt(OffsetDateTime.now()); users.save(legacy);
		LegacyIdentityClaimEntity claim = new LegacyIdentityClaimEntity();
		claim.setInstallationId(installationId); claim.setAnonymousUser(legacy); claim.setClaimedByUser(account); claims.save(claim);
	}

	private static AuthException conflict() { return new AuthException(HttpStatus.CONFLICT, "LEGACY_IDENTITY_ALREADY_CLAIMED"); }
}
