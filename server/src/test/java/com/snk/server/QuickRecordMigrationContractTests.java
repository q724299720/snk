package com.snk.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class QuickRecordMigrationContractTests {

	@Test
	void migrationShouldAllowUnknownItemsAndCreateIdempotencyIndex() throws Exception {
		Path migration = Path.of("src/main/resources/db/migration/V11__add_quick_record_idempotency.sql");
		assertThat(migration).exists();
		String sql = Files.readString(migration);
		assertThat(sql).contains("'unknown'");
		assertThat(sql).contains("client_request_id UUID");
		assertThat(sql).contains("uk_food_records_user_client_request");
		assertThat(sql).contains("WHERE client_request_id IS NOT NULL");
	}
}
