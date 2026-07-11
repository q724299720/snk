package com.snk.server;

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
import com.snk.server.infrastructure.security.AuthProperties;
import com.snk.server.infrastructure.security.OwnerBootstrapProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
	"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class ServerApplicationTests {

	@Autowired
	private AuthProperties authProperties;

	@Autowired
	private OwnerBootstrapProperties ownerBootstrapProperties;

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
		assertThat(authProperties.accessTokenTtl()).isEqualTo(Duration.ofMinutes(15));
		assertThat(authProperties.refreshGracePeriod()).isEqualTo(Duration.ofSeconds(60));
		assertThat(authProperties.jwtPrivateKey()).isNull();
		assertThat(authProperties.jwtPublicKey()).isNull();
		assertThat(authProperties.tokenEncryptionKey()).isNull();
		assertThat(ownerBootstrapProperties.username()).isNull();
		assertThat(ownerBootstrapProperties.password()).isNull();
		assertThat(ownerBootstrapProperties.forceReset()).isFalse();
	}

}
