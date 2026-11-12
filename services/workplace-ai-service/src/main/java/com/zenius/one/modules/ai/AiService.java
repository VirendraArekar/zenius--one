package com.zenius.one.modules.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zenius.one.modules.analytics.AnalyticsService;
import com.zenius.one.modules.automation.AutomationService;
import com.zenius.one.modules.identity.security.PermissionGuard;
import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.itsecurity.AuditLogService;
import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.common.TenantContext;
import com.zenius.one.platform.notifications.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
public class AiService {

    private static final List<AgentDescriptor> AGENTS = List.of(
            new AgentDescriptor("ASSISTANT", "Zenius Global Assistant", "Cross-app enterprise copilot grounded in permissions & RAG", "ai:read"),
            new AgentDescriptor("SALES", "Sales Revenue Agent", "Deal closing coaching, quote margin checks, and win probability guidance", "crm:deal:read"),
            new AgentDescriptor("CRM", "CRM Intelligence Agent", "Lead qualification, relationship timelines, and churn signals", "crm:lead:read"),
            new AgentDescriptor("FINANCE", "Finance & CFO Agent", "Receivables aging, cashflow risk, GST/TDS anomalies, and GL insights", "finance:read"),
            new AgentDescriptor("HR", "HR & People Agent", "Workforce policy Q&A, leave utilization, and engagement risk", "hr:read"),
            new AgentDescriptor("RECRUITMENT", "Talent & ATS Agent", "Candidate ranking rubrics, JD drafting, and interview scorecards", "recruitment:read"),
            new AgentDescriptor("MARKETING", "Growth & Campaign Agent", "Audience segmentation, campaign ROI, and journey optimization", "marketing:read"),
            new AgentDescriptor("SUPPORT", "Customer Support Agent", "Ticket triage, SLA breach alerts, and KB-grounded replies", "support:ticket:read"),
            new AgentDescriptor("PROJECT", "Delivery & PM Agent", "Sprint velocity, blocker detection, and billable utilization", "projects:read"),
            new AgentDescriptor("ANALYTICS", "BI & Forecast Agent", "Metric variance explanations and executive KPI commentary", "analytics:read"),
            new AgentDescriptor("DOCUMENT", "Document & Legal Agent", "Contract clause review, retention policies, and policy search", "docs:read"),
            new AgentDescriptor("WORKFLOW", "Workflow Architect Agent", "Natural-language to HITL-approved workflow automation rules", "automation:write")
    );

    private final AiKnowledgeDocRepository docRepository;
    private final AiKnowledgeChunkRepository chunkRepository;
    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final AiActionProposalRepository proposalRepository;
    private final AnalyticsService analyticsService;
    private final AutomationService automationService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final PermissionGuard permissionGuard;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final String aiProvider;
    private final String aiBaseUrl;
    private final String aiModel;
    private final String aiApiKey;

    public AiService(
            AiKnowledgeDocRepository docRepository,
            AiKnowledgeChunkRepository chunkRepository,
            AiConversationRepository conversationRepository,
            AiMessageRepository messageRepository,
            AiActionProposalRepository proposalRepository,
            AnalyticsService analyticsService,
            AutomationService automationService,
            NotificationService notificationService,
            AuditLogService auditLogService,
            PermissionGuard permissionGuard,
            ObjectMapper objectMapper,
            JdbcTemplate jdbcTemplate,
            @Value("${zenius.ai.provider:local}") String aiProvider,
            @Value("${zenius.ai.base-url:http://localhost:11434}") String aiBaseUrl,
            @Value("${zenius.ai.model:llama3.2}") String aiModel,
            @Value("${zenius.ai.api-key:}") String aiApiKey) {
        this.docRepository = docRepository;
        this.chunkRepository = chunkRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.proposalRepository = proposalRepository;
        this.analyticsService = analyticsService;
        this.automationService = automationService;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.permissionGuard = permissionGuard;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.aiProvider = aiProvider;
        this.aiBaseUrl = aiBaseUrl;
        this.aiModel = aiModel;
        this.aiApiKey = aiApiKey;
    }

    public List<AgentDescriptor> listAgents() {
        return AGENTS;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDocSummary> listKnowledgeDocs() {
        UUID orgId = TenantContext.requireOrganizationId();
        return docRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(KnowledgeDocSummary::from)
                .toList();
    }

    @Transactional
    public KnowledgeDocSummary ingestKnowledge(
            ZeniusUserPrincipal principal, @Valid IngestKnowledgeRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        List<String> chunks = VectorEmbeddingHelper.chunkText(request.contentText(), 320, 48);

        AiKnowledgeDocEntity doc = new AiKnowledgeDocEntity();
        doc.setOrganizationId(orgId);
        doc.setTitle(request.title().trim());
        doc.setSourceType(
                request.sourceType() == null || request.sourceType().isBlank()
                        ? "GENERAL"
                        : request.sourceType().trim().toUpperCase(Locale.ROOT));
        doc.setContentText(request.contentText().trim());
        doc.setChunkCount(chunks.size());
        AiKnowledgeDocEntity savedDoc = docRepository.save(doc);

        for (int i = 0; i < chunks.size(); i++) {
            AiKnowledgeChunkEntity chunk = new AiKnowledgeChunkEntity();
            chunk.setOrganizationId(orgId);
            chunk.setDocumentId(savedDoc.getId());
            chunk.setChunkIndex(i);
            chunk.setChunkText(chunks.get(i));
            chunk.setEmbeddingJson(toJson(VectorEmbeddingHelper.embed(chunks.get(i))));
            chunkRepository.save(chunk);
        }

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "AI_KNOWLEDGE_INGESTED",
                "AI_KNOWLEDGE_DOC",
                savedDoc.getId().toString(),
                "Indexed " + chunks.size() + " RAG chunks for " + savedDoc.getTitle(),
                "SUCCESS");

        return KnowledgeDocSummary.from(savedDoc);
    }

    @Transactional(readOnly = true)
    public List<RagCitation> searchKnowledge(String query, int topK) {
        UUID orgId = TenantContext.requireOrganizationId();
        if (query == null || query.isBlank()) {
            return List.of();
        }
        double[] queryVec = VectorEmbeddingHelper.embed(query);
        Map<UUID, AiKnowledgeDocEntity> docMap = new LinkedHashMap<>();
        docRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).forEach(d -> docMap.put(d.getId(), d));

        List<AiKnowledgeChunkEntity> chunks = chunkRepository.findByOrganizationId(orgId);
        List<RagCitation> scored = new ArrayList<>();
        String qLower = query.toLowerCase(Locale.ROOT);
        for (AiKnowledgeChunkEntity chunk : chunks) {
            double[] vec = parseVector(chunk.getEmbeddingJson());
            double cosine = VectorEmbeddingHelper.cosine(queryVec, vec);
            double lexicalBoost = containsAnyTerm(chunk.getChunkText().toLowerCase(Locale.ROOT), qLower) ? 0.25 : 0.0;
            double score = cosine + lexicalBoost;
            if (score > 0.05) {
                AiKnowledgeDocEntity doc = docMap.get(chunk.getDocumentId());
                scored.add(new RagCitation(
                        chunk.getDocumentId(),
                        doc != null ? doc.getTitle() : "Knowledge Doc",
                        chunk.getChunkIndex(),
                        chunk.getChunkText(),
                        Math.min(1.0, Math.round(score * 1000.0) / 1000.0)));
            }
        }
        scored.sort(Comparator.comparingDouble(RagCitation::score).reversed());
        return scored.stream().limit(Math.max(1, topK)).toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationSummary> listConversations(ZeniusUserPrincipal principal) {
        UUID orgId = TenantContext.requireOrganizationId();
        return conversationRepository
                .findByOrganizationIdAndUserIdOrderByUpdatedAtDesc(orgId, principal.getUserId())
                .stream()
                .map(ConversationSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationDetail getConversation(ZeniusUserPrincipal principal, UUID conversationId) {
        UUID orgId = TenantContext.requireOrganizationId();
        AiConversationEntity conv = conversationRepository
                .findByIdAndOrganizationId(conversationId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        if (!conv.getUserId().equals(principal.getUserId())) {
            throw new IllegalArgumentException("Conversation belongs to another user");
        }
        List<MessageSummary> messages = messageRepository
                .findByOrganizationIdAndConversationIdOrderByCreatedAtAsc(orgId, conv.getId())
                .stream()
                .map(this::toMessageSummary)
                .toList();
        return new ConversationDetail(ConversationSummary.from(conv), messages);
    }

    @Transactional
    public ChatResponse chat(ZeniusUserPrincipal principal, @Valid ChatRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        String agentCode = normalizeAgent(request.agentCode());

        AiConversationEntity conv;
        if (request.conversationId() != null) {
            conv = conversationRepository
                    .findByIdAndOrganizationId(request.conversationId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        } else {
            conv = new AiConversationEntity();
            conv.setOrganizationId(orgId);
            conv.setUserId(principal.getUserId());
            conv.setAgentCode(agentCode);
            conv.setTitle(truncate(request.message().trim(), 64));
            conv = conversationRepository.save(conv);
        }

        AiMessageEntity userMsg = new AiMessageEntity();
        userMsg.setOrganizationId(orgId);
        userMsg.setConversationId(conv.getId());
        userMsg.setRole("USER");
        userMsg.setContent(request.message().trim());
        messageRepository.save(userMsg);

        List<RagCitation> citations = searchKnowledge(request.message(), 3);
        String permissionContext = buildPermissionAwareContext(orgId);
        ActionProposalSummary proposal = maybeCreateProposal(orgId, conv.getId(), agentCode, request.message());

        String reply = generateReply(agentCode, request.message(), permissionContext, citations, proposal);

        AiMessageEntity assistantMsg = new AiMessageEntity();
        assistantMsg.setOrganizationId(orgId);
        assistantMsg.setConversationId(conv.getId());
        assistantMsg.setRole("ASSISTANT");
        assistantMsg.setContent(reply);
        assistantMsg.setCitationsJson(toJson(citations));
        assistantMsg.setToolCallsJson(toJson(proposal != null ? List.of(proposal.actionType()) : List.of()));
        AiMessageEntity savedAssistant = messageRepository.save(assistantMsg);

        return new ChatResponse(conv.getId(), agentCode, toMessageSummary(savedAssistant), citations, proposal);
    }

    @Transactional(readOnly = true)
    public List<ActionProposalSummary> listProposals() {
        UUID orgId = TenantContext.requireOrganizationId();
        return proposalRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(ActionProposalSummary::from)
                .toList();
    }

    @Transactional
    public ActionProposalSummary approveProposal(ZeniusUserPrincipal principal, UUID proposalId) {
        UUID orgId = TenantContext.requireOrganizationId();
        AiActionProposalEntity proposal = proposalRepository
                .findByIdAndOrganizationId(proposalId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));
        if (!"PENDING".equalsIgnoreCase(proposal.getStatus())) {
            throw new IllegalArgumentException("Proposal is already " + proposal.getStatus());
        }

        Map<String, Object> payload = parseMap(proposal.getPayloadJson());
        if ("CREATE_WORKFLOW".equalsIgnoreCase(proposal.getActionType())) {
            if (!permissionGuard.hasPermission("automation:write")) {
                throw new IllegalArgumentException("Missing automation:write permission to execute workflow proposal");
            }
            automationService.createWorkflow(
                    principal,
                    new AutomationService.CreateWorkflowRequest(
                            stringOrDefault(payload.get("name"), proposal.getSummary()),
                            stringOrDefault(payload.get("triggerEvent"), "CRM_DEAL_WON"),
                            stringOrDefault(payload.get("conditionExpr"), "amount>1000"),
                            stringOrDefault(payload.get("actionType"), "CREATE_NOTIFICATION"),
                            "{}",
                            true));
        } else {
            notificationService.send(
                    orgId,
                    principal.getUserId(),
                    "AI Executed: " + proposal.getActionType(),
                    proposal.getSummary(),
                    "AI_COPILOT",
                    "/ai");
        }

        proposal.setStatus("EXECUTED");
        proposal.setApprovedBy(principal.getEmail());
        proposal.setExecutedAt(Instant.now());
        AiActionProposalEntity saved = proposalRepository.save(proposal);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "AI_PROPOSAL_EXECUTED",
                "AI_PROPOSAL",
                saved.getId().toString(),
                "Approved & executed AI action: " + saved.getSummary(),
                "SUCCESS");

        return ActionProposalSummary.from(saved);
    }

    @Transactional
    public ActionProposalSummary rejectProposal(ZeniusUserPrincipal principal, UUID proposalId) {
        UUID orgId = TenantContext.requireOrganizationId();
        AiActionProposalEntity proposal = proposalRepository
                .findByIdAndOrganizationId(proposalId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));
        if (!"PENDING".equalsIgnoreCase(proposal.getStatus())) {
            throw new IllegalArgumentException("Proposal is already " + proposal.getStatus());
        }
        proposal.setStatus("REJECTED");
        proposal.setApprovedBy(principal.getEmail());
        return ActionProposalSummary.from(proposalRepository.save(proposal));
    }

    @Transactional(readOnly = true)
    public List<DomainInsight> getDomainInsights() {
        UUID orgId = TenantContext.requireOrganizationId();
        List<DomainInsight> insights = new ArrayList<>();

        if (permissionGuard.hasPermission("crm:deal:read")) {
            Map<String, Object> deals = jdbcTemplate.queryForMap(
                    "SELECT COUNT(*) AS cnt, COALESCE(SUM(amount), 0) AS amt, COALESCE(SUM(expected_revenue), 0) AS exp FROM crm_deals WHERE organization_id = ? AND deleted_at IS NULL",
                    orgId);
            long dealCount = toLong(deals.get("cnt"));
            BigDecimal pipeline = toDecimal(deals.get("amt"));
            BigDecimal expected = toDecimal(deals.get("exp"));
            insights.add(new DomainInsight(
                    "CRM_SALES",
                    "Pipeline Win Forecast",
                    dealCount + " active deals totaling $" + pipeline + " (weighted forecast $" + expected + ").",
                    dealCount == 0 ? "LOW" : "INFO"));
        }

        if (permissionGuard.hasPermission("finance:read")) {
            Map<String, Object> inv = jdbcTemplate.queryForMap(
                    "SELECT COUNT(*) AS cnt, COALESCE(SUM(balance_due), 0) AS due FROM fin_invoices WHERE organization_id = ?",
                    orgId);
            long invCount = toLong(inv.get("cnt"));
            BigDecimal due = toDecimal(inv.get("due"));
            String risk = due.compareTo(new BigDecimal("10000")) > 0 ? "MEDIUM" : "LOW";
            insights.add(new DomainInsight(
                    "FINANCE",
                    "Receivables & Cashflow Exposure",
                    invCount + " invoices tracked with $" + due + " outstanding receivable balance.",
                    risk));
        }

        if (permissionGuard.hasPermission("support:ticket:read")) {
            long totalTickets = queryCount("SELECT COUNT(*) FROM sup_tickets WHERE organization_id = ?", orgId);
            long openHigh = queryCount(
                    "SELECT COUNT(*) FROM sup_tickets WHERE organization_id = ? AND UPPER(status) <> 'CLOSED' AND UPPER(priority) IN ('HIGH', 'URGENT')",
                    orgId);
            insights.add(new DomainInsight(
                    "SUPPORT",
                    "SLA & Escalation Triage",
                    totalTickets + " total tickets; " + openHigh + " open high/urgent priority tickets requiring attention.",
                    openHigh > 0 ? "HIGH" : "LOW"));
        }

        if (permissionGuard.hasPermission("hr:read")) {
            long staff = queryCount("SELECT COUNT(*) FROM hr_employees WHERE organization_id = ?", orgId);
            insights.add(new DomainInsight(
                    "HR",
                    "Workforce Capacity Snapshot",
                    staff + " active employees on roster across organization departments.",
                    "INFO"));
        }

        return insights;
    }

    private ActionProposalSummary maybeCreateProposal(
            UUID orgId, UUID conversationId, String agentCode, String prompt) {
        String lower = prompt.toLowerCase(Locale.ROOT);
        boolean wantsWorkflow = lower.contains("workflow") || lower.contains("automate") || lower.contains("automation");
        if (!wantsWorkflow && !"WORKFLOW".equals(agentCode)) {
            return null;
        }
        String trigger = lower.contains("ticket")
                ? "SUPPORT_TICKET_CREATED"
                : lower.contains("invoice") ? "INVOICE_OVERDUE" : "CRM_DEAL_WON";
        Map<String, Object> payload = Map.of(
                "name", "AI Proposed: " + truncate(prompt.trim(), 48),
                "triggerEvent", trigger,
                "conditionExpr", lower.contains("high") ? "priority==HIGH" : "amount>1000",
                "actionType", "CREATE_NOTIFICATION");

        AiActionProposalEntity prop = new AiActionProposalEntity();
        prop.setOrganizationId(orgId);
        prop.setConversationId(conversationId);
        prop.setAgentCode(agentCode);
        prop.setActionType("CREATE_WORKFLOW");
        prop.setSummary("Create workflow rule triggered by " + trigger);
        prop.setPayloadJson(toJson(payload));
        prop.setStatus("PENDING");
        return ActionProposalSummary.from(proposalRepository.save(prop));
    }

    private String buildPermissionAwareContext(UUID orgId) {
        List<String> parts = new ArrayList<>();
        if (permissionGuard.hasPermission("analytics:read")) {
            AnalyticsService.ExecutivePulse pulse = analyticsService.getExecutivePulse();
            parts.add("CRM deals=" + pulse.dealCount() + " ($" + pulse.pipelineValue() + ")");
            parts.add("Invoiced=$" + pulse.totalInvoiced() + " (AR due $" + pulse.totalReceivable() + ")");
            parts.add("Open tickets=" + pulse.openTickets());
            parts.add("Headcount=" + pulse.employeeHeadcount());
        } else {
            if (permissionGuard.hasPermission("crm:deal:read")) {
                parts.add("CRM deals=" + queryCount("SELECT COUNT(*) FROM crm_deals WHERE organization_id = ? AND deleted_at IS NULL", orgId));
            }
            if (permissionGuard.hasPermission("support:ticket:read")) {
                parts.add("Tickets=" + queryCount("SELECT COUNT(*) FROM sup_tickets WHERE organization_id = ?", orgId));
            }
        }
        return parts.isEmpty() ? "Limited scope (user lacks cross-app analytics permissions)." : String.join("; ", parts);
    }

    private String generateReply(
            String agentCode,
            String prompt,
            String permissionContext,
            List<RagCitation> citations,
            ActionProposalSummary proposal) {
        if ("ollama".equalsIgnoreCase(aiProvider) || "openai".equalsIgnoreCase(aiProvider)) {
            String remote = tryExternalLlm(agentCode, prompt, permissionContext, citations);
            if (remote != null && !remote.isBlank()) {
                return remote;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[").append(agentCode).append(" Agent] ");
        sb.append("Live tenant telemetry: ").append(permissionContext).append(". ");
        if (!citations.isEmpty()) {
            sb.append("RAG Policy Evidence from \"")
                    .append(citations.get(0).documentTitle())
                    .append("\": \"")
                    .append(truncate(citations.get(0).snippet(), 180))
                    .append("\". ");
        } else {
            sb.append("No matching knowledge-base policy snippets found for this query. ");
        }
        if (proposal != null) {
            sb.append("Proposed HITL action [").append(proposal.actionType()).append("]: ")
                    .append(proposal.summary())
                    .append(" — awaiting your explicit approval.");
        }
        return sb.toString();
    }

    private String tryExternalLlm(
            String agentCode, String prompt, String permissionContext, List<RagCitation> citations) {
        try {
            RestClient client = RestClient.builder().baseUrl(aiBaseUrl).build();
            String systemPrompt = "You are Zenius One " + agentCode + " Agent. Live metrics: " + permissionContext
                    + ". Citations count: " + citations.size();
            if ("ollama".equalsIgnoreCase(aiProvider)) {
                Map<String, Object> body = Map.of(
                        "model", aiModel,
                        "prompt", systemPrompt + "\nUser: " + prompt,
                        "stream", false);
                Map<?, ?> resp = client.post()
                        .uri("/api/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(Map.class);
                if (resp != null && resp.get("response") != null) {
                    return resp.get("response").toString();
                }
            } else {
                Map<String, Object> body = Map.of(
                        "model", aiModel,
                        "messages", List.of(
                                Map.of("role", "system", "content", systemPrompt),
                                Map.of("role", "user", "content", prompt)));
                RestClient.RequestBodySpec spec = client.post()
                        .uri("/v1/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON);
                if (aiApiKey != null && !aiApiKey.isBlank()) {
                    spec = spec.header("Authorization", "Bearer " + aiApiKey);
                }
                Map<?, ?> resp = spec.body(body).retrieve().body(Map.class);
                if (resp != null && resp.get("choices") instanceof List<?> choices && !choices.isEmpty()) {
                    Object first = choices.get(0);
                    if (first instanceof Map<?, ?> choice && choice.get("message") instanceof Map<?, ?> msg) {
                        Object content = msg.get("content");
                        if (content != null) {
                            return content.toString();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Fall back cleanly when external LLM is unavailable
        }
        return null;
    }

    private long queryCount(String sql, UUID orgId) {
        Long val = jdbcTemplate.queryForObject(sql, Long.class, orgId);
        return val == null ? 0L : val;
    }

    private long toLong(Object val) {
        return val instanceof Number n ? n.longValue() : 0L;
    }

    private BigDecimal toDecimal(Object val) {
        if (val instanceof BigDecimal bd) {
            return bd;
        }
        return val == null ? BigDecimal.ZERO : new BigDecimal(val.toString());
    }

    private String normalizeAgent(String raw) {
        if (raw == null || raw.isBlank()) {
            return "ASSISTANT";
        }
        String norm = raw.trim().toUpperCase(Locale.ROOT);
        boolean valid = AGENTS.stream().anyMatch(a -> a.code().equals(norm));
        if (!valid) {
            throw new IllegalArgumentException("Unknown AI agent: " + raw);
        }
        return norm;
    }

    private boolean containsAnyTerm(String hayLower, String queryLower) {
        for (String term : queryLower.split("\\s+")) {
            if (term.length() >= 3 && hayLower.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    private String stringOrDefault(Object v, String fallback) {
        return v == null || v.toString().isBlank() ? fallback : v.toString();
    }

    private double[] parseVector(String json) {
        try {
            return objectMapper.readValue(json, double[].class);
        } catch (Exception ex) {
            return new double[0];
        }
    }

    private Map<String, Object> parseMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private List<RagCitation> parseCitations(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private MessageSummary toMessageSummary(AiMessageEntity entity) {
        return new MessageSummary(
                entity.getId(),
                entity.getRole(),
                entity.getContent(),
                parseCitations(entity.getCitationsJson()),
                entity.getCreatedAt());
    }

    public record AgentDescriptor(String code, String name, String description, String requiredPermission) {}

    public record IngestKnowledgeRequest(
            @NotBlank @Size(max = 180) String title, String sourceType, @NotBlank String contentText) {}

    public record SearchKnowledgeRequest(@NotBlank String query, Integer topK) {}

    public record ChatRequest(UUID conversationId, String agentCode, @NotBlank String message) {}

    public record KnowledgeDocSummary(
            UUID id, String title, String sourceType, int chunkCount, Instant createdAt) {
        public static KnowledgeDocSummary from(AiKnowledgeDocEntity e) {
            return new KnowledgeDocSummary(e.getId(), e.getTitle(), e.getSourceType(), e.getChunkCount(), e.getCreatedAt());
        }
    }

    public record RagCitation(UUID documentId, String documentTitle, int chunkIndex, String snippet, double score) {}

    public record ConversationSummary(UUID id, String title, String agentCode, Instant updatedAt) {
        public static ConversationSummary from(AiConversationEntity e) {
            return new ConversationSummary(e.getId(), e.getTitle(), e.getAgentCode(), e.getUpdatedAt());
        }
    }

    public record MessageSummary(
            UUID id, String role, String content, List<RagCitation> citations, Instant createdAt) {}

    public record ConversationDetail(ConversationSummary conversation, List<MessageSummary> messages) {}

    public record ActionProposalSummary(
            UUID id,
            UUID conversationId,
            String agentCode,
            String actionType,
            String summary,
            String status,
            String approvedBy,
            Instant executedAt,
            Instant createdAt) {
        public static ActionProposalSummary from(AiActionProposalEntity e) {
            return new ActionProposalSummary(
                    e.getId(),
                    e.getConversationId(),
                    e.getAgentCode(),
                    e.getActionType(),
                    e.getSummary(),
                    e.getStatus(),
                    e.getApprovedBy(),
                    e.getExecutedAt(),
                    e.getCreatedAt());
        }
    }

    public record ChatResponse(
            UUID conversationId,
            String agentCode,
            MessageSummary reply,
            List<RagCitation> citations,
            ActionProposalSummary proposal) {}

    public record DomainInsight(String domain, String title, String summary, String severity) {}
}
