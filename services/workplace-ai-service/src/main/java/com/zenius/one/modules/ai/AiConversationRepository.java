package com.zenius.one.modules.ai;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiConversationRepository extends JpaRepository<AiConversationEntity, UUID> {
    List<AiConversationEntity> findByOrganizationIdAndUserIdOrderByUpdatedAtDesc(UUID organizationId, UUID userId);

    Optional<AiConversationEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}

