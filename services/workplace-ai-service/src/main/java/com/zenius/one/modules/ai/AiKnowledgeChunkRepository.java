package com.zenius.one.modules.ai;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiKnowledgeChunkRepository extends JpaRepository<AiKnowledgeChunkEntity, UUID> {
    List<AiKnowledgeChunkEntity> findByOrganizationId(UUID organizationId);
}

