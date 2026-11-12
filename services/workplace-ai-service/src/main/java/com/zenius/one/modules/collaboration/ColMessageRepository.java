package com.zenius.one.modules.collaboration;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColMessageRepository extends JpaRepository<ColMessageEntity, UUID> {
    List<ColMessageEntity> findByOrganizationIdAndChannelIdOrderByCreatedAtAsc(UUID organizationId, UUID channelId);
}

