package com.snk.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class FlywayMigrationIntegrationTests {

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

	@Test
	void flywayShouldCreateCoreTablesAndExtensions() {
		assertThat(countTable("users")).isEqualTo(1);
		assertThat(countTable("food_items")).isEqualTo(1);
		assertThat(countTable("food_item_reports")).isEqualTo(1);
		assertThat(countTable("food_images")).isEqualTo(1);
		assertThat(countTable("food_records")).isEqualTo(1);
		assertThat(countTable("food_record_images")).isEqualTo(1);
		assertThat(countTable("recognition_tasks")).isEqualTo(1);
		assertThat(countTable("tags")).isEqualTo(1);
		assertThat(countTable("review_config_words")).isEqualTo(1);
		assertThat(countTable("review_config_word_audit_logs")).isEqualTo(1);
		assertThat(countExtension("pg_trgm")).isEqualTo(1);
	}

	@Test
	void flywayShouldCreateExpectedConstraintsAndIndexes() {
		List<String> indexes = jdbcTemplate.queryForList("""
			SELECT indexname
			FROM pg_indexes
			WHERE schemaname = 'public'
			  AND tablename = 'food_items'
			""", String.class);

		assertThat(indexes).contains(
			"uk_food_items_packaged_barcode",
			"idx_food_items_name_trgm",
			"idx_food_items_alias_trgm",
			"idx_food_items_search_keywords_trgm"
		);

		Integer checkConstraintCount = jdbcTemplate.queryForObject("""
			SELECT COUNT(*)
			FROM information_schema.table_constraints
			WHERE table_schema = 'public'
			  AND table_name = 'food_records'
			  AND constraint_type = 'CHECK'
			""", Integer.class);

		assertThat(checkConstraintCount).isNotNull();
		assertThat(checkConstraintCount).isGreaterThanOrEqualTo(4);
	}

	private Integer countTable(String tableName) {
		return jdbcTemplate.queryForObject("""
			SELECT COUNT(*)
			FROM information_schema.tables
			WHERE table_schema = 'public'
			  AND table_name = ?
			""", Integer.class, tableName);
	}

	private Integer countExtension(String extensionName) {
		return jdbcTemplate.queryForObject("""
			SELECT COUNT(*)
			FROM pg_extension
			WHERE extname = ?
			""", Integer.class, extensionName);
	}
}
