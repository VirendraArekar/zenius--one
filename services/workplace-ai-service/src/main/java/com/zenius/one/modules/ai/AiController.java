package com.zenius.one.modules.ai;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.platform.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "Zenius AI", description = "Global conversational AI assistant, 11 specialized domain agents, RAG knowledge base, and HITL proposals")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/agents")
    @PreAuthorize("@authz.hasPermission('ai:read')")
    public ApiResponse<List<AiService.AgentDescriptor>> listAgents() {
        return ApiResponse.ok(aiService.listAgents());
    }

    @GetMapping("/insights")
    @PreAuthorize("@authz.hasPermission('ai:read')")
    public ApiResponse<List<AiService.DomainInsight>> getInsights() {
        return ApiResponse.ok(aiService.getDomainInsights());
    }

    @GetMapping("/knowledge")
    @PreAuthorize("@authz.hasPermission('ai:read')")
    public ApiResponse<List<AiService.KnowledgeDocSummary>> listKnowledge() {
        return ApiResponse.ok(aiService.listKnowledgeDocs());
    }

    @PostMapping("/knowledge")
    @PreAuthorize("@authz.hasPermission('ai:write')")
    public ApiResponse<AiService.KnowledgeDocSummary> ingestKnowledge(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody AiService.IngestKnowledgeRequest request) {
        return ApiResponse.ok(aiService.ingestKnowledge(principal, request));
    }

    @PostMapping("/knowledge/search")
    @PreAuthorize("@authz.hasPermission('ai:read')")
    public ApiResponse<List<AiService.RagCitation>> searchKnowledge(
            @Valid @RequestBody AiService.SearchKnowledgeRequest request) {
        return ApiResponse.ok(
                aiService.searchKnowledge(request.query(), request.topK() == null ? 5 : request.topK()));
    }

    @GetMapping("/conversations")
    @PreAuthorize("@authz.hasPermission('ai:read')")
    public ApiResponse<List<AiService.ConversationSummary>> listConversations(
            @AuthenticationPrincipal ZeniusUserPrincipal principal) {
        return ApiResponse.ok(aiService.listConversations(principal));
    }

    @GetMapping("/conversations/{id}")
    @PreAuthorize("@authz.hasPermission('ai:read')")
    public ApiResponse<AiService.ConversationDetail> getConversation(
            @AuthenticationPrincipal ZeniusUserPrincipal principal, @PathVariable UUID id) {
        return ApiResponse.ok(aiService.getConversation(principal, id));
    }

    @PostMapping("/chat")
    @PreAuthorize("@authz.hasPermission('ai:read')")
    public ApiResponse<AiService.ChatResponse> chat(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody AiService.ChatRequest request) {
        return ApiResponse.ok(aiService.chat(principal, request));
    }

    @GetMapping("/proposals")
    @PreAuthorize("@authz.hasPermission('ai:read')")
    public ApiResponse<List<AiService.ActionProposalSummary>> listProposals() {
        return ApiResponse.ok(aiService.listProposals());
    }

    @PostMapping("/proposals/{id}/approve")
    @PreAuthorize("@authz.hasPermission('ai:write')")
    public ApiResponse<AiService.ActionProposalSummary> approveProposal(
            @AuthenticationPrincipal ZeniusUserPrincipal principal, @PathVariable UUID id) {
        return ApiResponse.ok(aiService.approveProposal(principal, id));
    }

    @PostMapping("/proposals/{id}/reject")
    @PreAuthorize("@authz.hasPermission('ai:write')")
    public ApiResponse<AiService.ActionProposalSummary> rejectProposal(
            @AuthenticationPrincipal ZeniusUserPrincipal principal, @PathVariable UUID id) {
        return ApiResponse.ok(aiService.rejectProposal(principal, id));
    }
}

