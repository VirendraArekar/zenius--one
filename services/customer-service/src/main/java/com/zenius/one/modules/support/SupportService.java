package com.zenius.one.modules.support;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.itsecurity.AuditLogService;
import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.common.TenantContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupportService {

    private final SupSlaPolicyRepository slaRepository;
    private final SupTicketRepository ticketRepository;
    private final SupTicketCommentRepository commentRepository;
    private final SupCannedResponseRepository cannedRepository;
    private final SupKbArticleRepository kbRepository;
    private final SupServiceAppointmentRepository appointmentRepository;
    private final AuditLogService auditLogService;

    public SupportService(
            SupSlaPolicyRepository slaRepository,
            SupTicketRepository ticketRepository,
            SupTicketCommentRepository commentRepository,
            SupCannedResponseRepository cannedRepository,
            SupKbArticleRepository kbRepository,
            SupServiceAppointmentRepository appointmentRepository,
            AuditLogService auditLogService
    ) {
        this.slaRepository = slaRepository;
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.cannedRepository = cannedRepository;
        this.kbRepository = kbRepository;
        this.appointmentRepository = appointmentRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public SupportDashboardDto getDashboard() {
        UUID orgId = TenantContext.requireOrganizationId();
        ensureDefaultSlas(orgId);
        List<SupTicketEntity> tickets = ticketRepository.findByOrganizationId(orgId);
        long open = tickets.stream().filter(t -> "OPEN".equals(t.getStatus()) || "IN_PROGRESS".equals(t.getStatus())).count();
        long resolved = tickets.stream().filter(t -> "RESOLVED".equals(t.getStatus()) || "CLOSED".equals(t.getStatus())).count();
        long escalated = tickets.stream().filter(SupTicketEntity::isEscalated).count();
        double avgCsat = tickets.stream()
                .filter(t -> t.getCsatRating() != null)
                .mapToInt(SupTicketEntity::getCsatRating)
                .average()
                .orElse(0.0);
        long publishedKb = kbRepository.countByOrganizationIdAndPublishedTrue(orgId);
        long appointments = appointmentRepository.countByOrganizationId(orgId);

        return new SupportDashboardDto(tickets.size(), open, resolved, escalated, avgCsat, publishedKb, appointments);
    }

    @Transactional
    public List<SlaPolicyDto> listSlaPolicies() {
        UUID orgId = TenantContext.requireOrganizationId();
        ensureDefaultSlas(orgId);
        return slaRepository.findByOrganizationIdOrderByResolutionMinutesAsc(orgId).stream()
                .map(s -> new SlaPolicyDto(s.getId(), s.getName(), s.getPriority(), s.getFirstResponseMinutes(), s.getResolutionMinutes()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<TicketDto> listTickets(String status, String priority, String query, Pageable pageable) {
        UUID orgId = TenantContext.requireOrganizationId();
        return ticketRepository.search(orgId, norm(status), norm(priority), norm(query), pageable).map(this::toTicketDto);
    }

    @Transactional
    public TicketDto createTicket(ZeniusUserPrincipal principal, CreateTicketRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        ensureDefaultSlas(orgId);

        String priority = req.priority() != null && !req.priority().isBlank()
                ? req.priority().trim().toUpperCase()
                : "MEDIUM";
        SupSlaPolicyEntity sla = slaRepository.findByOrganizationIdAndPriorityIgnoreCase(orgId, priority)
                .orElse(null);

        long seq = ticketRepository.countByOrganizationId(orgId) + 1;
        SupTicketEntity ticket = new SupTicketEntity();
        ticket.setOrganizationId(orgId);
        ticket.setTicketNumber(String.format("TKT-%05d", seq));
        ticket.setSubject(req.subject().trim());
        ticket.setDescription(req.description().trim());
        ticket.setChannel(req.channel() != null && !req.channel().isBlank() ? req.channel().trim().toUpperCase() : "WEB");
        ticket.setPriority(priority);
        ticket.setStatus("OPEN");
        ticket.setRequesterName(req.requesterName().trim());
        ticket.setRequesterEmail(req.requesterEmail().trim().toLowerCase());
        ticket.setAssigneeUserId(req.assigneeUserId());
        if (sla != null) {
            ticket.setSlaDueAt(Instant.now().plus(sla.getResolutionMinutes(), ChronoUnit.MINUTES));
        }
        ticket = ticketRepository.save(ticket);

        auditLogService.record(orgId, principal.getUserId(), principal.getEmail(),
                "SUP_TICKET_CREATED", "SUPPORT_TICKET", ticket.getId().toString(),
                "Created ticket " + ticket.getTicketNumber() + " (" + ticket.getPriority() + ")", "SUCCESS");
        return toTicketDto(ticket);
    }

    @Transactional
    public TicketDto updateTicketStatus(ZeniusUserPrincipal principal, UUID ticketId, String status, boolean escalate) {
        UUID orgId = TenantContext.requireOrganizationId();
        SupTicketEntity ticket = ticketRepository.findByIdAndOrganizationId(ticketId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        String nextStatus = status.trim().toUpperCase();
        ticket.setStatus(nextStatus);
        if (escalate) {
            ticket.setEscalated(true);
        }
        if (("RESOLVED".equals(nextStatus) || "CLOSED".equals(nextStatus)) && ticket.getResolvedAt() == null) {
            ticket.setResolvedAt(Instant.now());
        }
        ticket = ticketRepository.save(ticket);

        auditLogService.record(orgId, principal.getUserId(), principal.getEmail(),
                "SUP_TICKET_UPDATED", "SUPPORT_TICKET", ticket.getId().toString(),
                "Updated ticket " + ticket.getTicketNumber() + " status to " + nextStatus, "SUCCESS");
        return toTicketDto(ticket);
    }

    @Transactional(readOnly = true)
    public List<CommentDto> listComments(UUID ticketId) {
        UUID orgId = TenantContext.requireOrganizationId();
        ticketRepository.findByIdAndOrganizationId(ticketId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));
        return commentRepository.findByOrganizationIdAndTicketIdOrderByCreatedAtAsc(orgId, ticketId).stream()
                .map(c -> new CommentDto(c.getId(), c.getTicketId(), c.getAuthorName(), c.getBody(), c.isInternalNote(), c.getCreatedAt()))
                .toList();
    }

    @Transactional
    public CommentDto addComment(ZeniusUserPrincipal principal, UUID ticketId, AddCommentRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        SupTicketEntity ticket = ticketRepository.findByIdAndOrganizationId(ticketId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        if (!req.internalNote() && ticket.getFirstRespondedAt() == null) {
            ticket.setFirstRespondedAt(Instant.now());
            if ("OPEN".equals(ticket.getStatus())) {
                ticket.setStatus("IN_PROGRESS");
            }
            ticketRepository.save(ticket);
        }

        SupTicketCommentEntity comment = new SupTicketCommentEntity();
        comment.setOrganizationId(orgId);
        comment.setTicketId(ticketId);
        comment.setAuthorUserId(principal.getUserId());
        comment.setAuthorName(principal.getFullName());
        comment.setBody(req.body().trim());
        comment.setInternalNote(req.internalNote());
        comment = commentRepository.save(comment);

        return new CommentDto(comment.getId(), comment.getTicketId(), comment.getAuthorName(), comment.getBody(), comment.isInternalNote(), comment.getCreatedAt());
    }

    @Transactional
    public TicketDto submitCsat(UUID ticketId, int rating, String feedback) {
        UUID orgId = TenantContext.requireOrganizationId();
        SupTicketEntity ticket = ticketRepository.findByIdAndOrganizationId(ticketId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));
        ticket.setCsatRating(rating);
        ticket.setCsatFeedback(norm(feedback));
        return toTicketDto(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public List<CannedResponseDto> listCannedResponses() {
        UUID orgId = TenantContext.requireOrganizationId();
        return cannedRepository.findByOrganizationIdOrderByTitleAsc(orgId).stream()
                .map(c -> new CannedResponseDto(c.getId(), c.getTitle(), c.getCategory(), c.getBody()))
                .toList();
    }

    @Transactional
    public CannedResponseDto createCannedResponse(CreateCannedRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        SupCannedResponseEntity c = new SupCannedResponseEntity();
        c.setOrganizationId(orgId);
        c.setTitle(req.title().trim());
        c.setCategory(norm(req.category()));
        c.setBody(req.body().trim());
        c = cannedRepository.save(c);
        return new CannedResponseDto(c.getId(), c.getTitle(), c.getCategory(), c.getBody());
    }

    @Transactional(readOnly = true)
    public Page<KbArticleDto> listKbArticles(String category, String query, Pageable pageable) {
        UUID orgId = TenantContext.requireOrganizationId();
        return kbRepository.search(orgId, norm(category), norm(query), pageable).map(this::toKbDto);
    }

    @Transactional
    public KbArticleDto createKbArticle(CreateKbRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        SupKbArticleEntity a = new SupKbArticleEntity();
        a.setOrganizationId(orgId);
        a.setTitle(req.title().trim());
        a.setCategory(req.category().trim());
        a.setContent(req.content().trim());
        a.setPublished(req.published());
        return toKbDto(kbRepository.save(a));
    }

    @Transactional
    public KbArticleDto voteKbHelpful(UUID articleId) {
        UUID orgId = TenantContext.requireOrganizationId();
        SupKbArticleEntity a = kbRepository.findByIdAndOrganizationId(articleId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("KB article not found: " + articleId));
        a.setHelpfulCount(a.getHelpfulCount() + 1);
        return toKbDto(kbRepository.save(a));
    }

    @Transactional(readOnly = true)
    public List<AppointmentDto> listAppointments() {
        UUID orgId = TenantContext.requireOrganizationId();
        return appointmentRepository.findByOrganizationIdOrderByScheduledStartAsc(orgId).stream()
                .map(this::toAppointmentDto)
                .toList();
    }

    @Transactional
    public AppointmentDto createAppointment(CreateAppointmentRequest req) {
        UUID orgId = TenantContext.requireOrganizationId();
        if (req.ticketId() != null && ticketRepository.findByIdAndOrganizationId(req.ticketId(), orgId).isEmpty()) {
            throw new IllegalArgumentException("Ticket not found in current organization");
        }
        long seq = appointmentRepository.countByOrganizationId(orgId) + 1;
        SupServiceAppointmentEntity a = new SupServiceAppointmentEntity();
        a.setOrganizationId(orgId);
        a.setTicketId(req.ticketId());
        a.setAppointmentNumber(String.format("FSV-%05d", seq));
        a.setCustomerName(req.customerName().trim());
        a.setServiceAddress(req.serviceAddress().trim());
        a.setTechnicianUserId(req.technicianUserId());
        a.setScheduledStart(req.scheduledStart() != null ? req.scheduledStart() : Instant.now().plus(1, ChronoUnit.DAYS));
        a.setStatus("SCHEDULED");
        a.setNotes(norm(req.notes()));
        return toAppointmentDto(appointmentRepository.save(a));
    }

    private void ensureDefaultSlas(UUID orgId) {
        if (!slaRepository.findByOrganizationIdOrderByResolutionMinutesAsc(orgId).isEmpty()) {
            return;
        }
        saveSla(orgId, "Critical SLA", "URGENT", 15, 240);
        saveSla(orgId, "High Priority SLA", "HIGH", 60, 480);
        saveSla(orgId, "Standard SLA", "MEDIUM", 240, 1440);
        saveSla(orgId, "Low Priority SLA", "LOW", 480, 2880);
    }

    private void saveSla(UUID orgId, String name, String priority, int firstRespMin, int resMin) {
        SupSlaPolicyEntity s = new SupSlaPolicyEntity();
        s.setOrganizationId(orgId);
        s.setName(name);
        s.setPriority(priority);
        s.setFirstResponseMinutes(firstRespMin);
        s.setResolutionMinutes(resMin);
        slaRepository.save(s);
    }

    private TicketDto toTicketDto(SupTicketEntity t) {
        return new TicketDto(t.getId(), t.getTicketNumber(), t.getSubject(), t.getDescription(), t.getChannel(), t.getPriority(), t.getStatus(), t.getRequesterName(), t.getRequesterEmail(), t.getAssigneeUserId(), t.getSlaDueAt(), t.getFirstRespondedAt(), t.getResolvedAt(), t.isEscalated(), t.getCsatRating(), t.getCsatFeedback(), t.getCreatedAt());
    }

    private KbArticleDto toKbDto(SupKbArticleEntity a) {
        return new KbArticleDto(a.getId(), a.getTitle(), a.getCategory(), a.getContent(), a.isPublished(), a.getHelpfulCount(), a.getCreatedAt());
    }

    private AppointmentDto toAppointmentDto(SupServiceAppointmentEntity a) {
        return new AppointmentDto(a.getId(), a.getTicketId(), a.getAppointmentNumber(), a.getCustomerName(), a.getServiceAddress(), a.getTechnicianUserId(), a.getScheduledStart(), a.getStatus(), a.getNotes());
    }

    private String norm(String val) {
        return (val == null || val.isBlank()) ? null : val.trim();
    }

    public record SupportDashboardDto(long totalTickets, long openTickets, long resolvedTickets, long escalatedTickets, double averageCsat, long publishedKbArticles, long fieldAppointments) {
    }

    public record SlaPolicyDto(UUID id, String name, String priority, Integer firstResponseMinutes, Integer resolutionMinutes) {
    }

    public record CreateTicketRequest(String subject, String description, String channel, String priority, String requesterName, String requesterEmail, UUID assigneeUserId) {
    }

    public record TicketDto(UUID id, String ticketNumber, String subject, String description, String channel, String priority, String status, String requesterName, String requesterEmail, UUID assigneeUserId, Instant slaDueAt, Instant firstRespondedAt, Instant resolvedAt, boolean escalated, Integer csatRating, String csatFeedback, Instant createdAt) {
    }

    public record AddCommentRequest(String body, boolean internalNote) {
    }

    public record CommentDto(UUID id, UUID ticketId, String authorName, String body, boolean internalNote, Instant createdAt) {
    }

    public record CreateCannedRequest(String title, String category, String body) {
    }

    public record CannedResponseDto(UUID id, String title, String category, String body) {
    }

    public record CreateKbRequest(String title, String category, String content, boolean published) {
    }

    public record KbArticleDto(UUID id, String title, String category, String content, boolean published, Integer helpfulCount, Instant createdAt) {
    }

    public record CreateAppointmentRequest(UUID ticketId, String customerName, String serviceAddress, UUID technicianUserId, Instant scheduledStart, String notes) {
    }

    public record AppointmentDto(UUID id, UUID ticketId, String appointmentNumber, String customerName, String serviceAddress, UUID technicianUserId, Instant scheduledStart, String status, String notes) {
    }
}

