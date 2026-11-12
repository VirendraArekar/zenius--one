package com.zenius.one.modules.collaboration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColMailMessageRepository extends JpaRepository<ColMailMessageEntity, UUID> {
    List<ColMailMessageEntity> findByOrganizationIdAndFolderOrderByCreatedAtDesc(UUID organizationId, String folder);
    List<ColMailMessageEntity> findByOrganizationIdAndStarredTrueOrderByCreatedAtDesc(UUID organizationId);
    List<ColMailMessageEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    Optional<ColMailMessageEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}

