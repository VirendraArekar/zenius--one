package com.zenius.one.platform.notifications;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.common.TenantContext;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final DomainEventBus domainEventBus;

    public NotificationService(NotificationRepository notificationRepository) {
        this(notificationRepository, null);
    }

    @Autowired
    public NotificationService(NotificationRepository notificationRepository, DomainEventBus domainEventBus) {
        this.notificationRepository = notificationRepository;
        this.domainEventBus = domainEventBus;
    }

    @Transactional
    public NotificationEntity send(
            UUID organizationId,
            UUID recipientUserId,
            String title,
            String message,
            String category,
            String linkPath
    ) {
        NotificationEntity entity = new NotificationEntity();
        entity.setOrganizationId(organizationId);
        entity.setRecipientUserId(recipientUserId);
        entity.setTitle(title);
        entity.setMessage(message);
        entity.setCategory(category != null ? category : "SYSTEM");
        entity.setLinkPath(linkPath);
        NotificationEntity saved = notificationRepository.save(entity);
        if (domainEventBus != null) {
            domainEventBus.publish(DomainEvent.notification(
                    organizationId, recipientUserId, title, message, entity.getCategory(), linkPath));
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public NotificationFeedDto listMyNotifications(ZeniusUserPrincipal principal, Pageable pageable) {
        UUID orgId = TenantContext.requireOrganizationId();
        Page<NotificationDto> page = notificationRepository
                .findByOrganizationIdAndRecipientUserIdOrderByCreatedAtDesc(orgId, principal.getUserId(), pageable)
                .map(this::toDto);
        long unreadCount = notificationRepository
                .countByOrganizationIdAndRecipientUserIdAndReadAtIsNull(orgId, principal.getUserId());
        return new NotificationFeedDto(unreadCount, page);
    }

    @Transactional
    public NotificationDto markRead(ZeniusUserPrincipal principal, UUID notificationId) {
        UUID orgId = TenantContext.requireOrganizationId();
        NotificationEntity entity = notificationRepository
                .findByIdAndOrganizationIdAndRecipientUserId(notificationId, orgId, principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        if (entity.getReadAt() == null) {
            entity.setReadAt(Instant.now());
            notificationRepository.save(entity);
        }
        return toDto(entity);
    }

    @Transactional
    public int markAllRead(ZeniusUserPrincipal principal) {
        UUID orgId = TenantContext.requireOrganizationId();
        List<NotificationEntity> unread = notificationRepository
                .findByOrganizationIdAndRecipientUserIdAndReadAtIsNull(orgId, principal.getUserId());
        Instant now = Instant.now();
        for (NotificationEntity n : unread) {
            n.setReadAt(now);
        }
        notificationRepository.saveAll(unread);
        return unread.size();
    }

    private NotificationDto toDto(NotificationEntity e) {
        return new NotificationDto(
                e.getId(),
                e.getTitle(),
                e.getMessage(),
                e.getCategory(),
                e.getLinkPath(),
                e.getReadAt() != null,
                e.getReadAt(),
                e.getCreatedAt()
        );
    }

    public record NotificationDto(
            UUID id,
            String title,
            String message,
            String category,
            String linkPath,
            boolean read,
            Instant readAt,
            Instant createdAt
    ) {
    }

    public record NotificationFeedDto(
            long unreadCount,
            Page<NotificationDto> page
    ) {
    }
}

