package com.snk.server.infrastructure.persistence.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationApprovalTicketRepository
	extends JpaRepository<RegistrationApprovalTicketEntity, Long> {

	Optional<RegistrationApprovalTicketEntity> findByTicketHash(String ticketHash);

	Optional<RegistrationApprovalTicketEntity> findByUser_Id(Long userId);
}
