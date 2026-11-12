package com.zenius.one.modules.collaboration;

import com.zenius.one.modules.collaboration.CollaborationService.ChannelDto;
import com.zenius.one.modules.collaboration.CollaborationService.CreateChannelRequest;
import com.zenius.one.modules.collaboration.CollaborationService.CreateMeetingRequest;
import com.zenius.one.modules.collaboration.CollaborationService.MailDto;
import com.zenius.one.modules.collaboration.CollaborationService.MeetingDto;
import com.zenius.one.modules.collaboration.CollaborationService.MessageDto;
import com.zenius.one.modules.collaboration.CollaborationService.SendMailRequest;
import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/collab")
@Tag(name = "Mail & Collaboration", description = "Business email, team chat channels, and video meetings")
public class CollaborationController {

    private final CollaborationService collabService;

    public CollaborationController(CollaborationService collabService) {
        this.collabService = collabService;
    }

    @GetMapping("/mail")
    @PreAuthorize("@authz.hasPermission('collab:read')")
    public ApiResponse<List<MailDto>> listMail(@RequestParam(required = false) String folder) {
        return ApiResponse.ok(collabService.listMail(folder));
    }

    @PostMapping("/mail")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('collab:write')")
    public ApiResponse<MailDto> sendMail(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody MailBody body
    ) {
        return ApiResponse.ok(collabService.sendMail(principal, new SendMailRequest(body.recipientEmail(), body.subject(), body.body(), body.labels())));
    }

    @PatchMapping("/mail/{id}/read")
    @PreAuthorize("@authz.hasPermission('collab:write')")
    public ApiResponse<MailDto> setMailRead(
            @PathVariable UUID id,
            @RequestBody(required = false) MailReadBody body
    ) {
        boolean read = body == null || body.read();
        return ApiResponse.ok(collabService.setMailRead(id, read));
    }

    @PatchMapping("/mail/{id}/star")
    @PreAuthorize("@authz.hasPermission('collab:write')")
    public ApiResponse<MailDto> toggleStar(@PathVariable UUID id) {
        return ApiResponse.ok(collabService.toggleStar(id));
    }

    @PostMapping("/mail/{id}/reply")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('collab:write')")
    public ApiResponse<MailDto> replyMail(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody MessageBody body
    ) {
        return ApiResponse.ok(collabService.replyMail(principal, id, body.body()));
    }

    @GetMapping("/channels")
    @PreAuthorize("@authz.hasPermission('collab:read')")
    public ApiResponse<List<ChannelDto>> listChannels() {
        return ApiResponse.ok(collabService.listChannels());
    }

    @PostMapping("/channels")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('collab:write')")
    public ApiResponse<ChannelDto> createChannel(@Valid @RequestBody ChannelBody body) {
        return ApiResponse.ok(collabService.createChannel(new CreateChannelRequest(body.name(), body.description(), body.privateChannel())));
    }

    @GetMapping("/channels/{channelId}/messages")
    @PreAuthorize("@authz.hasPermission('collab:read')")
    public ApiResponse<List<MessageDto>> listMessages(@PathVariable UUID channelId) {
        return ApiResponse.ok(collabService.listMessages(channelId));
    }

    @PostMapping("/channels/{channelId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('collab:write')")
    public ApiResponse<MessageDto> postMessage(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID channelId,
            @Valid @RequestBody MessageBody body
    ) {
        return ApiResponse.ok(collabService.postMessage(principal, channelId, body.body()));
    }

    @GetMapping("/meetings")
    @PreAuthorize("@authz.hasPermission('collab:read')")
    public ApiResponse<List<MeetingDto>> listMeetings() {
        return ApiResponse.ok(collabService.listMeetings());
    }

    @PostMapping("/meetings")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('collab:write')")
    public ApiResponse<MeetingDto> scheduleMeeting(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody MeetingBody body
    ) {
        return ApiResponse.ok(collabService.scheduleMeeting(principal, new CreateMeetingRequest(body.title(), body.scheduledAt(), body.durationMinutes())));
    }

    public record MailBody(@NotBlank @Email String recipientEmail, @NotBlank String subject, @NotBlank String body, String labels) {
    }

    public record MailReadBody(boolean read) {
    }

    public record ChannelBody(@NotBlank String name, String description, boolean privateChannel) {
    }

    public record MessageBody(@NotBlank String body) {
    }

    public record MeetingBody(@NotBlank String title, Instant scheduledAt, Integer durationMinutes) {
    }
}

