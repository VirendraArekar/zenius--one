package com.zenius.one.platform.notifications;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.platform.common.ApiResponse;
import com.zenius.one.platform.common.TenantContext;
import com.zenius.one.platform.notifications.NotificationService.NotificationDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifications")
@Validated
@Tag(name = "Notifications", description = "Centralized tenant-scoped notification inbox")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationStreamService notificationStreamService;
    private final DomainEventBus domainEventBus;

    public NotificationController(
            NotificationService notificationService,
            NotificationStreamService notificationStreamService,
            DomainEventBus domainEventBus
    ) {
        this.notificationService = notificationService;
        this.notificationStreamService = notificationStreamService;
        this.domainEventBus = domainEventBus;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("@authz.hasPermission('notification:read')")
    @Operation(summary = "Real-time notification event stream (SSE over event bus)")
    public SseEmitter streamNotifications(@AuthenticationPrincipal ZeniusUserPrincipal principal) {
        UUID orgId = TenantContext.requireOrganizationId();
        return notificationStreamService.subscribe(orgId, principal.getUserId());
    }

    @GetMapping("/broker")
    @PreAuthorize("@authz.hasPermission('notification:read')")
    @Operation(summary = "Active event broker transport mode")
    public ApiResponse<Map<String, String>> brokerStatus() {
        return ApiResponse.ok(Map.of("transport", domainEventBus.getBrokerMode()));
    }

    @GetMapping
    @PreAuthorize("@authz.hasPermission('notification:read')")
    @Operation(summary = "List personal notifications in active organization")
    public ApiResponse<NotificationListResponse> listNotifications(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        var feed = notificationService.listMyNotifications(principal, PageRequest.of(page, size));
        return ApiResponse.page(
                feed.page(),
                new NotificationListResponse(feed.unreadCount(), feed.page().getContent())
        );
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("@authz.hasPermission('notification:read')")
    @Operation(summary = "Mark a notification as read")
    public ApiResponse<NotificationDto> markRead(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id
    ) {
        return ApiResponse.ok(notificationService.markRead(principal, id));
    }

    @PostMapping("/read-all")
    @PreAuthorize("@authz.hasPermission('notification:read')")
    @Operation(summary = "Mark all unread notifications as read")
    public ApiResponse<Map<String, Integer>> markAllRead(
            @AuthenticationPrincipal ZeniusUserPrincipal principal
    ) {
        int updated = notificationService.markAllRead(principal);
        return ApiResponse.ok(Map.of("updated", updated));
    }

    public record NotificationListResponse(long unreadCount, List<NotificationDto> items) {
    }
}

