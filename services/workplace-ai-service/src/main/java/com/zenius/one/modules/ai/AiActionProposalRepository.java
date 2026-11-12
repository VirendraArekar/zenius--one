package com.zenius.one.modules.ai;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiActionProposalRepository extends JpaRepository<AiActionProposalEntity, UUID> {
    List<AiActionProposalEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<AiActionProposalEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}

