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
import com.snk.server.infrastructure.persistence.auth.UploadedObjectRepository;
import com.snk.server.infrastructure.persistence.auth.AccountAuditLogRepository;
import com.snk.server.infrastructure.persistence.auth.RefreshTokenRepository;
import com.snk.server.infrastructure.persistence.auth.RegistrationApprovalTicketRepository;
import com.snk.server.infrastructure.persistence.auth.LegacyIdentityClaimRepository;
import com.snk.server.domain.food.FoodSearchService;
import com.snk.server.domain.recognition.ImageRecognitionTaskProvider;
import com.snk.server.infrastructure.security.AuthProperties;
import com.snk.server.infrastructure.security.OwnerBootstrapProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
	"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
	"snk.auth.enforce-security=true"
})
@AutoConfigureMockMvc
class ServerApplicationTests {

	@Autowired
	private AuthProperties authProperties;

	@Autowired
	private OwnerBootstrapProperties ownerBootstrapProperties;

	@Autowired
	private SecurityFilterChain securityFilterChain;

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private UserRepository userRepository;

	@MockBean
	private UploadedObjectRepository uploadedObjectRepository;

	@MockBean
	private RegistrationApprovalTicketRepository registrationApprovalTicketRepository;

	@MockBean
	private RefreshTokenRepository refreshTokenRepository;

	@MockBean
	private AccountAuditLogRepository accountAuditLogRepository;

	@MockBean
	private LegacyIdentityClaimRepository legacyIdentityClaimRepository;

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
		assertThat(securityFilterChain).isNotNull();
		assertThat(authProperties.accessTokenTtl()).isEqualTo(Duration.ofMinutes(15));
		assertThat(authProperties.refreshGracePeriod()).isEqualTo(Duration.ofSeconds(60));
		assertThat(authProperties.jwtPrivateKey()).isNull();
		assertThat(authProperties.jwtPublicKey()).isNull();
		assertThat(authProperties.tokenEncryptionKey()).isNull();
		assertThat(ownerBootstrapProperties.username()).isNull();
		assertThat(ownerBootstrapProperties.password()).isNull();
		assertThat(ownerBootstrapProperties.forceReset()).isFalse();
	}

	@Test
	void protectedApiRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/records/public")).andExpect(status().isUnauthorized());
	}

	@Test
	void adminApiRejectsNonOwnerRole() throws Exception {
		mockMvc.perform(get("/api/admin/stats").with(user("member").roles("USER")))
			.andExpect(status().isForbidden());
	}

}
