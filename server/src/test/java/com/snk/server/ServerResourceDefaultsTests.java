package com.snk.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

class ServerResourceDefaultsTests {

	@Test
	void usesResourceConsciousSingleNodeDefaults() throws IOException {
		PropertySource<?> properties = new YamlPropertySourceLoader()
			.load("application", new FileSystemResource("src/main/resources/application.yml"))
			.getFirst();

		assertThat(properties.getProperty("server.tomcat.threads.max")).isEqualTo("${SNK_TOMCAT_MAX_THREADS:24}");
		assertThat(properties.getProperty("server.tomcat.threads.min-spare")).isEqualTo("${SNK_TOMCAT_MIN_SPARE_THREADS:2}");
		assertThat(properties.getProperty("server.tomcat.max-connections")).isEqualTo("${SNK_TOMCAT_MAX_CONNECTIONS:64}");
		assertThat(properties.getProperty("spring.datasource.hikari.maximum-pool-size")).isEqualTo("${SNK_DB_MAX_POOL_SIZE:3}");
		assertThat(properties.getProperty("spring.datasource.hikari.minimum-idle")).isEqualTo("${SNK_DB_MIN_IDLE:0}");
		assertThat(properties.getProperty("spring.servlet.multipart.file-size-threshold")).isEqualTo("0B");
	}
}
