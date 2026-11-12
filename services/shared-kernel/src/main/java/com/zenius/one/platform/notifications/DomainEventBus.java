package com.zenius.one.platform.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Multi-transport Domain Event Bus publishing real-time notification and workflow events
 * via Spring ApplicationEventPublisher + SSE fanout, and optional webhook/external broker relay.
 */
@Component
public class DomainEventBus {

    private static final Logger log = LoggerFactory.getLogger(DomainEventBus.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;
    private final String brokerMode;
    private final String webhookUrl;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public DomainEventBus(
            ApplicationEventPublisher applicationEventPublisher,
            ObjectMapper objectMapper,
            @Value("${zenius.events.broker:local}") String brokerMode,
            @Value("${zenius.events.webhook-url:}") String webhookUrl
    ) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.objectMapper = objectMapper;
        this.brokerMode = brokerMode;
        this.webhookUrl = webhookUrl;
    }

    public String getBrokerMode() {
        if (webhookUrl != null && !webhookUrl.isBlank()) {
            return brokerMode.toUpperCase() + "_SSE_BRIDGE";
        }
        return "REALTIME_SSE_EVENT_BUS";
    }

    public void publish(DomainEvent event) {
        applicationEventPublisher.publishEvent(event);
        if (webhookUrl != null && !webhookUrl.isBlank()) {
            executor.submit(() -> dispatchWebhookBridge(event));
        } else {
            log.debug("Event published on local event bus: topic={} type={} org={}",
                    event.topic(), event.eventType(), event.organizationId());
        }
    }

    private void dispatchWebhookBridge(DomainEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header("X-Event-Topic", event.topic())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ex) {
            log.warn("Event bridge relay failed for {}: {}", event.eventId(), ex.getMessage());
        }
    }
}
