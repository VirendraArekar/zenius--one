package com.zenius.one.platform.notifications;

import java.time.Instant;
import java.util.UUID;

public record DomainEvent(
        UUID eventId,
        UUID organizationId,
        UUID recipientUserId,
        String topic,
        String eventType,
        String title,
        String message,
        String category,
        String linkPath,
        Instant occurredAt
) {
    public static DomainEvent notification(
            UUID organizationId,
            UUID recipientUserId,
            String title,
            String message,
            String category,
            String linkPath
    ) {
        return new DomainEvent(
                UUID.randomUUID(),
                organizationId,
                recipientUserId,
                "zenius.notifications.events",
                "NOTIFICATION_CREATED",
                title,
                message,
                category != null ? category : "SYSTEM",
                linkPath,
                Instant.now()
        );
    }
}

