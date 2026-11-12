package com.zenius.one.modules.support;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupTicketCommentRepository extends JpaRepository<SupTicketCommentEntity, UUID> {
    List<SupTicketCommentEntity> findByOrganizationIdAndTicketIdOrderByCreatedAtAsc(UUID organizationId, UUID ticketId);
}

