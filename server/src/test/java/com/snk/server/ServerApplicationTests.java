package com.snk.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.snk.server.infrastructure.persistence.food.FoodItemRepository;
import com.snk.server.infrastructure.persistence.food.FoodItemReportRepository;
import com.snk.server.infrastructure.persistence.review.ReviewConfigWordAuditLogRepository;
import com.snk.server.infrastructure.persistence.review.ReviewConfigWordRepository;
import com.snk.server.infrastructure.persistence.record.FoodRecordCommentRepository;
import com.snk.server.infrastructure.persistence.record.FoodRecordImageRepository;
import com.snk.server.infrastructure.persistence.record.FoodRecordRepository;
import com.snk.server.infrastructure.persistence.recognition.RecognitionTaskRepository;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import com.snk.server.domain.food.FoodSearchService;
import com.snk.server.domain.recognition.ImageRecognitionTaskProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;

@SpringBootTest(properties = {
	"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class ServerApplicationTests {

	@Autowired
	private Environment environment;

	@MockBean
	private UserRepository userRepository;

	@MockBean
	private FoodItemRepository foodItemRepository;

	@MockBean
	private FoodItemReportRepository foodItemReportRepository;

	@MockBean
	private FoodRecordRepository foodRecordRepository;

	@MockBean
	private FoodRecordImageRepository foodRecordImageRepository;

	@MockBean
	private FoodRecordCommentRepository foodRecordCommentRepository;

	@MockBean
	private RecognitionTaskRepository recognitionTaskRepository;

	@MockBean
	private FoodSearchService foodSearchService;

	@MockBean
	private ImageRecognitionTaskProvider imageRecognitionTaskProvider;

	@MockBean
	private ReviewConfigWordRepository reviewConfigWordRepository;

	@MockBean
	private ReviewConfigWordAuditLogRepository reviewConfigWordAuditLogRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void usesResourceConsciousSingleNodeDefaults() {
		assertThat(environment.getProperty("server.tomcat.threads.max", Integer.class)).isEqualTo(24);
		assertThat(environment.getProperty("server.tomcat.threads.min-spare", Integer.class)).isEqualTo(2);
		assertThat(environment.getProperty("server.tomcat.max-connections", Integer.class)).isEqualTo(64);
		assertThat(environment.getProperty("spring.datasource.hikari.maximum-pool-size", Integer.class)).isEqualTo(3);
		assertThat(environment.getProperty("spring.datasource.hikari.minimum-idle", Integer.class)).isZero();
	}

}
