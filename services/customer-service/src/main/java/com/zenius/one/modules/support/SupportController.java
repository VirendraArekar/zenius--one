package com.zenius.one.modules.support;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.support.SupportService.AddCommentRequest;
import com.zenius.one.modules.support.SupportService.AppointmentDto;
import com.zenius.one.modules.support.SupportService.CannedResponseDto;
import com.zenius.one.modules.support.SupportService.CommentDto;
import com.zenius.one.modules.support.SupportService.CreateAppointmentRequest;
import com.zenius.one.modules.support.SupportService.CreateCannedRequest;
import com.zenius.one.modules.support.SupportService.CreateKbRequest;
import com.zenius.one.modules.support.SupportService.CreateTicketRequest;
import com.zenius.one.modules.support.SupportService.KbArticleDto;
import com.zenius.one.modules.support.SupportService.SlaPolicyDto;
import com.zenius.one.modules.support.SupportService.SupportDashboardDto;
import com.zenius.one.modules.support.SupportService.TicketDto;
import com.zenius.one.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping("/api/v1/support")
@Validated
@Tag(name = "Customer Support & Field Service", description = "Multi-channel tickets, SLA policies, KB articles, CSAT, and field appointments")
public class SupportController {

    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("@authz.hasPermission('support:ticket:read')")
    public ApiResponse<SupportDashboardDto> dashboard() {
        return ApiResponse.ok(supportService.getDashboard());
    }

    @GetMapping("/slas")
    @PreAuthorize("@authz.hasPermission('support:ticket:read')")
    public ApiResponse<List<SlaPolicyDto>> listSlas() {
        return ApiResponse.ok(supportService.listSlaPolicies());
    }

    @GetMapping("/tickets")
    @PreAuthorize("@authz.hasPermission('support:ticket:read')")
    public ApiResponse<List<TicketDto>> listTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size
    ) {
        Page<TicketDto> res = supportService.listTickets(
                status, priority, q, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return ApiResponse.page(res, res.getContent());
    }

    @PostMapping("/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('support:ticket:write')")
    public ApiResponse<TicketDto> createTicket(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody TicketBody body
    ) {
        return ApiResponse.ok(supportService.createTicket(
                principal,
                new CreateTicketRequest(
                        body.subject(), body.description(), body.channel(), body.priority(),
                        body.requesterName(), body.requesterEmail(), body.assigneeUserId()
                )
        ));
    }

    @PatchMapping("/tickets/{id}/status")
    @PreAuthorize("@authz.hasPermission('support:ticket:write')")
    public ApiResponse<TicketDto> updateTicketStatus(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody TicketStatusBody body
    ) {
        return ApiResponse.ok(supportService.updateTicketStatus(principal, id, body.status(), body.escalate()));
    }

    @GetMapping("/tickets/{id}/comments")
    @PreAuthorize("@authz.hasPermission('support:ticket:read')")
    public ApiResponse<List<CommentDto>> listComments(@PathVariable UUID id) {
        return ApiResponse.ok(supportService.listComments(id));
    }

    @PostMapping("/tickets/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('support:ticket:write')")
    public ApiResponse<CommentDto> addComment(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody CommentBody body
    ) {
        return ApiResponse.ok(supportService.addComment(principal, id, new AddCommentRequest(body.body(), body.internalNote())));
    }

    @PostMapping("/tickets/{id}/csat")
    @PreAuthorize("@authz.hasPermission('support:ticket:write')")
    public ApiResponse<TicketDto> submitCsat(@PathVariable UUID id, @Valid @RequestBody CsatBody body) {
        return ApiResponse.ok(supportService.submitCsat(id, body.rating(), body.feedback()));
    }

    @GetMapping("/canned-responses")
    @PreAuthorize("@authz.hasPermission('support:kb:read')")
    public ApiResponse<List<CannedResponseDto>> listCanned() {
        return ApiResponse.ok(supportService.listCannedResponses());
    }

    @PostMapping("/canned-responses")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('support:kb:write')")
    public ApiResponse<CannedResponseDto> createCanned(@Valid @RequestBody CannedBody body) {
        return ApiResponse.ok(supportService.createCannedResponse(new CreateCannedRequest(body.title(), body.category(), body.body())));
    }

    @GetMapping("/kb")
    @PreAuthorize("@authz.hasPermission('support:kb:read')")
    public ApiResponse<List<KbArticleDto>> listKb(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size
    ) {
        Page<KbArticleDto> res = supportService.listKbArticles(category, q, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.page(res, res.getContent());
    }

    @PostMapping("/kb")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('support:kb:write')")
    public ApiResponse<KbArticleDto> createKb(@Valid @RequestBody KbBody body) {
        return ApiResponse.ok(supportService.createKbArticle(new CreateKbRequest(body.title(), body.category(), body.content(), body.published())));
    }

    @PostMapping("/kb/{id}/helpful")
    @PreAuthorize("@authz.hasPermission('support:kb:read')")
    public ApiResponse<KbArticleDto> voteHelpful(@PathVariable UUID id) {
        return ApiResponse.ok(supportService.voteKbHelpful(id));
    }

    @GetMapping("/appointments")
    @PreAuthorize("@authz.hasPermission('support:field:read')")
    public ApiResponse<List<AppointmentDto>> listAppointments() {
        return ApiResponse.ok(supportService.listAppointments());
    }

    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasPermission('support:field:write')")
    public ApiResponse<AppointmentDto> createAppointment(@Valid @RequestBody AppointmentBody body) {
        return ApiResponse.ok(supportService.createAppointment(
                new CreateAppointmentRequest(body.ticketId(), body.customerName(), body.serviceAddress(), body.technicianUserId(), body.scheduledStart(), body.notes())
        ));
    }

    public record TicketBody(
            @NotBlank String subject,
            @NotBlank String description,
            String channel,
            String priority,
            @NotBlank String requesterName,
            @NotBlank @Email String requesterEmail,
            UUID assigneeUserId
    ) {
    }

    public record TicketStatusBody(@NotBlank String status, boolean escalate) {
    }

    public record CommentBody(@NotBlank String body, boolean internalNote) {
    }

    public record CsatBody(@Min(1) @Max(5) int rating, String feedback) {
    }

    public record CannedBody(@NotBlank String title, String category, @NotBlank String body) {
    }

    public record KbBody(@NotBlank String title, @NotBlank String category, @NotBlank String content, boolean published) {
    }

    public record AppointmentBody(UUID ticketId, @NotBlank String customerName, @NotBlank String serviceAddress, UUID technicianUserId, Instant scheduledStart, String notes) {
    }
}

