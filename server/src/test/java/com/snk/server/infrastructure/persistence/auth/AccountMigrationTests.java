package com.snk.server.infrastructure.persistence.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.snk.server.infrastructure.persistence.user.AccountRole;
import com.snk.server.infrastructure.persistence.user.AccountStatus;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

class AccountMigrationTests {

	@Test
	void migrationScriptsShouldDeclareAccountAndSessionConstraints() throws Exception {
		String accountSql = Files.readString(Path.of(
			"src/main/resources/db/migration/V12__add_account_authentication.sql"
		));
		String sessionSql = Files.readString(Path.of(
			"src/main/resources/db/migration/V13__add_refresh_sessions_and_claims.sql"
		));

		assertThat(accountSql)
			.contains("LOWER(username)")
			.contains("WHERE username IS NOT NULL")
			.contains("account_status")
			.contains("must_change_password");
		assertThat(sessionSql)
			.contains("CREATE TABLE refresh_tokens")
			.contains("token_hash")
			.contains("replacement_ciphertext")
			.contains("CREATE TABLE legacy_identity_claims")
			.contains("CREATE TABLE registration_approval_tickets")
			.contains("CREATE TABLE account_audit_logs");
	}

	@Test
	void accountEnumsShouldExposeOnlySupportedValues() {
		assertThat(AccountRole.values()).containsExactly(AccountRole.OWNER, AccountRole.USER);
		assertThat(AccountStatus.values()).containsExactly(
			AccountStatus.PENDING,
			AccountStatus.ACTIVE,
			AccountStatus.REJECTED,
			AccountStatus.DISABLED
		);
	}

	@Test
	void repositoriesShouldExposeLockingAndIdentityLookups() throws Exception {
		assertThat(UserRepository.class.getMethod("findByUsernameIgnoreCase", String.class)).isNotNull();
		assertThat(UserRepository.class.getMethod(
			"findByRoleAndAccountStatusForUpdate",
			AccountRole.class,
			AccountStatus.class
		)).isNotNull();
		assertThat(RefreshTokenRepository.class.getMethod("findByTokenHashForUpdate", String.class)).isNotNull();
		assertThat(LegacyIdentityClaimRepository.class.getMethod("findByInstallationId", String.class)).isNotNull();
		assertThat(RegistrationApprovalTicketRepository.class.getMethod("findByTicketHash", String.class)).isNotNull();
		assertThat(AccountAuditLogRepository.class.getMethod(
			"findByTargetUser_IdOrderByCreatedAtDesc",
			Long.class,
			Pageable.class
		)).isNotNull();
	}

	@Test
	void accountAuditJsonSnapshotsShouldUsePostgresJsonBinding() throws Exception {
		assertThat(AccountAuditLogEntity.class.getDeclaredField("beforeState").getAnnotation(JdbcTypeCode.class))
			.extracting(JdbcTypeCode::value)
			.isEqualTo(SqlTypes.JSON);
		assertThat(AccountAuditLogEntity.class.getDeclaredField("afterState").getAnnotation(JdbcTypeCode.class))
			.extracting(JdbcTypeCode::value)
			.isEqualTo(SqlTypes.JSON);
	}
}

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@Transactional
class AccountMigrationPostgresTests {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
		.withDatabaseName("snk")
		.withUsername("snk")
		.withPassword("snk");

	@DynamicPropertySource
	static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
	}

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AccountAuditLogRepository accountAuditLogRepository;

	@Test
	void usernameShouldBeUniqueIgnoringCase() {
		insertFormalUser("Alice", "OWNER", "ACTIVE");

		assertThatThrownBy(() -> insertFormalUser("aLiCe", "USER", "PENDING"))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void roleShouldRejectUnknownValues() {
		assertThatThrownBy(() -> insertFormalUser("invalid-role", "ADMIN", "ACTIVE"))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void accountStatusShouldRejectUnknownValues() {
		assertThatThrownBy(() -> insertFormalUser("invalid-status", "USER", "UNKNOWN"))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void refreshTokenHashShouldBeUnique() {
		Long userId = insertFormalUser("refresh-owner", "OWNER", "ACTIVE");
		String tokenHash = "a".repeat(64);
		insertRefreshToken(userId, tokenHash);

		assertThatThrownBy(() -> insertRefreshToken(userId, tokenHash))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void anonymousInstallationShouldOnlyBeClaimedOnce() {
		Long anonymousUserId = jdbcTemplate.queryForObject("""
			INSERT INTO users (auth_provider, status, anonymous_installation_id)
			VALUES ('anonymous', 'active', 'legacy-installation')
			RETURNING id
			""", Long.class);
		Long firstAccountId = insertFormalUser("claim-owner", "OWNER", "ACTIVE");
		Long secondAccountId = insertFormalUser("claim-user", "USER", "ACTIVE");
		insertClaim("legacy-installation", anonymousUserId, firstAccountId);

		assertThatThrownBy(() -> insertClaim("legacy-installation", anonymousUserId, secondAccountId))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void approvalTicketHashShouldBeUnique() {
		Long firstAccountId = insertFormalUser("ticket-owner", "OWNER", "ACTIVE");
		Long secondAccountId = insertFormalUser("ticket-user", "USER", "PENDING");
		String ticketHash = "b".repeat(64);
		insertApprovalTicket(firstAccountId, ticketHash);

		assertThatThrownBy(() -> insertApprovalTicket(secondAccountId, ticketHash))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void accountAuditShouldPersistJsonSnapshots() {
		Long ownerId = insertFormalUser("audit-owner", "OWNER", "ACTIVE");
		UserEntity owner = userRepository.findById(ownerId).orElseThrow();
		AccountAuditLogEntity auditLog = new AccountAuditLogEntity();
		auditLog.setActorUser(owner);
		auditLog.setTargetUser(owner);
		auditLog.setAction(AccountAuditAction.CHANGE_PASSWORD);
		auditLog.setBeforeState("{\"tokenVersion\":0}");
		auditLog.setAfterState("{\"tokenVersion\":1}");

		AccountAuditLogEntity saved = accountAuditLogRepository.saveAndFlush(auditLog);

		assertThat(saved.getId()).isNotNull();
	}

	@Test
	void activeOwnersShouldBeQueryableWithPessimisticLock() {
		Long ownerId = insertFormalUser("locked-owner", "OWNER", "ACTIVE");

		List<UserEntity> owners = userRepository.findByRoleAndAccountStatusForUpdate(
			AccountRole.OWNER,
			AccountStatus.ACTIVE
		);

		assertThat(owners).extracting(UserEntity::getId).contains(ownerId);
		assertThat(userRepository.countByRoleAndAccountStatus(AccountRole.OWNER, AccountStatus.ACTIVE))
			.isGreaterThanOrEqualTo(1);
	}

	private Long insertFormalUser(String username, String role, String accountStatus) {
		return jdbcTemplate.queryForObject("""
			INSERT INTO users (
				username, password_hash, role, account_status,
				auth_provider, status, token_version, must_change_password
			)
			VALUES (?, 'bcrypt-hash', ?, ?, 'password', 'active', 0, false)
			RETURNING id
			""", Long.class, username, role, accountStatus);
	}

	private void insertRefreshToken(Long userId, String tokenHash) {
		jdbcTemplate.update("""
			INSERT INTO refresh_tokens (user_id, token_hash, family_id, device_id)
			VALUES (?, ?, gen_random_uuid(), 'test-device')
			""", userId, tokenHash);
	}

	private void insertClaim(String installationId, Long anonymousUserId, Long claimedByUserId) {
		jdbcTemplate.update("""
			INSERT INTO legacy_identity_claims (
				installation_id, anonymous_user_id, claimed_by_user_id
			)
			VALUES (?, ?, ?)
			""", installationId, anonymousUserId, claimedByUserId);
	}

	private void insertApprovalTicket(Long userId, String ticketHash) {
		jdbcTemplate.update("""
			INSERT INTO registration_approval_tickets (user_id, ticket_hash)
			VALUES (?, ?)
			""", userId, ticketHash);
	}
}
