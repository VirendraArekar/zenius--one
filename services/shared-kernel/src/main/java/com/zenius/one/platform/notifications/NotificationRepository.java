package com.zenius.one.platform.notifications;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    List<NotificationEntity> findByCreatedAtAfterOrderByCreatedAtAsc(Instant after);

    Page<NotificationEntity> findByOrganizationIdAndRecipientUserIdOrderByCreatedAtDesc(
            UUID organizationId,
            UUID recipientUserId,
            Pageable pageable
    );

    long countByOrganizationIdAndRecipientUserIdAndReadAtIsNull(UUID organizationId, UUID recipientUserId);

    List<NotificationEntity> findByOrganizationIdAndRecipientUserIdAndReadAtIsNull(
            UUID organizationId,
            UUID recipientUserId
    );

    Optional<NotificationEntity> findByIdAndOrganizationIdAndRecipientUserId(
            UUID id,
            UUID organizationId,
            UUID recipientUserId
    );
}

