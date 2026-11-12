package com.zenius.one.modules.ai;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiMessageRepository extends JpaRepository<AiMessageEntity, UUID> {
    List<AiMessageEntity> findByOrganizationIdAndConversationIdOrderByCreatedAtAsc(
            UUID organizationId, UUID conversationId);
}

