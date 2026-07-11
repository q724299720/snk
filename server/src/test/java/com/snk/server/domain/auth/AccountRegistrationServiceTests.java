package com.snk.server.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.snk.server.infrastructure.persistence.auth.RegistrationApprovalTicketEntity;
import com.snk.server.infrastructure.persistence.auth.RegistrationApprovalTicketRepository;
import com.snk.server.infrastructure.persistence.user.AccountRole;
import com.snk.server.infrastructure.persistence.user.AccountStatus;
import com.snk.server.infrastructure.persistence.user.UserEntity;
import com.snk.server.infrastructure.persistence.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AccountRegistrationServiceTests {

	@Mock
	private UserRepository userRepository;

	@Mock
	private RegistrationApprovalTicketRepository ticketRepository;

	private BCryptPasswordEncoder passwordEncoder;
	private AccountRegistrationService registrationService;

	@BeforeEach
	void setUp() {
		passwordEncoder = new BCryptPasswordEncoder(12);
		registrationService = new AccountRegistrationService(userRepository, ticketRepository, passwordEncoder);
	}

	@Test
	void registerShouldCreatePendingUserAndStoreOnlyTicketHash() {
		when(userRepository.findByUsernameIgnoreCase("Alice")).thenReturn(Optional.empty());
		when(userRepository.saveAndFlush(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(ticketRepository.save(any(RegistrationApprovalTicketEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		RegistrationResult result = registrationService.register("  Alice  ", "correct-horse-12");

		ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
		verify(userRepository).saveAndFlush(userCaptor.capture());
		UserEntity savedUser = userCaptor.getValue();
		assertThat(savedUser.getUsername()).isEqualTo("Alice");
		assertThat(savedUser.getRole()).isEqualTo(AccountRole.USER);
		assertThat(savedUser.getAccountStatus()).isEqualTo(AccountStatus.PENDING);
		assertThat(savedUser.getAuthProvider()).isEqualTo("password");
		assertThat(savedUser.getPasswordHash()).startsWith("$2a$12$");
		assertThat(passwordEncoder.matches("correct-horse-12", savedUser.getPasswordHash())).isTrue();

		ArgumentCaptor<RegistrationApprovalTicketEntity> ticketCaptor =
			ArgumentCaptor.forClass(RegistrationApprovalTicketEntity.class);
		verify(ticketRepository).save(ticketCaptor.capture());
		assertThat(result.approvalTicket()).isNotBlank();
		assertThat(ticketCaptor.getValue().getTicketHash())
			.hasSize(64)
			.isNotEqualTo(result.approvalTicket());
	}

	@Test
	void registerShouldRejectDuplicateUsernameIgnoringCase() {
		when(userRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(new UserEntity()));

		assertThatThrownBy(() -> registrationService.register("alice", "correct-horse-12"))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
			.isEqualTo(409);
	}

	@Test
	void registrationStatusShouldResolveHashedTicketWithoutExposingCredentials() {
		UserEntity user = new UserEntity();
		user.setAccountStatus(AccountStatus.REJECTED);
		RegistrationApprovalTicketEntity ticket = new RegistrationApprovalTicketEntity();
		ticket.setUser(user);
		when(ticketRepository.findByTicketHash(SecretHashing.sha256("approval-ticket")))
			.thenReturn(Optional.of(ticket));

		RegistrationStatusResult result = registrationService.getRegistrationStatus("approval-ticket");

		assertThat(result.accountStatus()).isEqualTo(AccountStatus.REJECTED);
	}

	@Test
	void registerShouldRejectPasswordOutsideBcryptBounds() {
		assertThatThrownBy(() -> registrationService.register("alice", "short"))
			.isInstanceOf(ResponseStatusException.class)
			.extracting(exception -> ((ResponseStatusException) exception).getStatusCode().value())
			.isEqualTo(400);
	}
}
