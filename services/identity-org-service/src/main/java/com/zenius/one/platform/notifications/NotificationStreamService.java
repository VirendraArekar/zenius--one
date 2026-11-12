package com.zenius.one.platform.notifications;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Manages live Server-Sent Events (SSE) streams for notifications across all microservices.
 * Pushes immediate local DomainEvents and polls cross-service DB notifications every 2.5s.
 */
@Service
public class NotificationStreamService {

    private record SubscriberKey(UUID organizationId, UUID userId) {}

    private final Map<SubscriberKey, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Set<UUID> dispatchedIds = ConcurrentHashMap.newKeySet();
    private final NotificationRepository notificationRepository;
    private volatile Instant lastCheckedAt = Instant.now().minusSeconds(2);

    public NotificationStreamService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public SseEmitter subscribe(UUID organizationId, UUID userId) {
        SseEmitter emitter = new SseEmitter(0L);
        SubscriberKey key = new SubscriberKey(organizationId, userId);
        emitters.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(key, emitter));
        emitter.onTimeout(() -> removeEmitter(key, emitter));
        emitter.onError(ex -> removeEmitter(key, emitter));

        try {
            long unread = notificationRepository
                    .countByOrganizationIdAndRecipientUserIdAndReadAtIsNull(organizationId, userId);
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("status", "CONNECTED", "unreadCount", unread)));
        } catch (IOException ex) {
            removeEmitter(key, emitter);
        }
        return emitter;
    }

    @EventListener
    public void onDomainEvent(DomainEvent event) {
        if (event.organizationId() == null || event.recipientUserId() == null) {
            return;
        }
        pushToSubscriber(new SubscriberKey(event.organizationId(), event.recipientUserId()), event);
    }

    @Scheduled(fixedDelay = 2500)
    public void dispatchCrossServiceNotifications() {
        if (emitters.isEmpty()) {
            lastCheckedAt = Instant.now();
            return;
        }
        Instant windowStart = lastCheckedAt.minusSeconds(1);
        Instant nextCursor = Instant.now();
        List<NotificationEntity> recent = notificationRepository.findByCreatedAtAfterOrderByCreatedAtAsc(windowStart);
        for (NotificationEntity entity : recent) {
            if (!dispatchedIds.add(entity.getId())) {
                continue;
            }
            DomainEvent event = new DomainEvent(
                    entity.getId(),
                    entity.getOrganizationId(),
                    entity.getRecipientUserId(),
                    "zenius.notifications.events",
                    "NOTIFICATION_CREATED",
                    entity.getTitle(),
                    entity.getMessage(),
                    entity.getCategory(),
                    entity.getLinkPath(),
                    entity.getCreatedAt() != null ? entity.getCreatedAt() : Instant.now()
            );
            pushToSubscriber(new SubscriberKey(entity.getOrganizationId(), entity.getRecipientUserId()), event);
        }
        if (dispatchedIds.size() > 2000) {
            dispatchedIds.clear();
        }
        lastCheckedAt = nextCursor;
    }

    private void pushToSubscriber(SubscriberKey key, DomainEvent event) {
        List<SseEmitter> list = emitters.get(key);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .id(event.eventId().toString())
                        .name("notification")
                        .data(event));
            } catch (IOException ex) {
                removeEmitter(key, emitter);
            }
        }
    }

    private void removeEmitter(SubscriberKey key, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(key);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(key);
            }
        }
    }
}

