package com.zenius.one.modules.documents;

import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.platform.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/docs")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/folders")
    @PreAuthorize("@authz.hasPermission('docs:read')")
    public ApiResponse<List<DocumentService.FolderSummary>> listFolders() {
        return ApiResponse.ok(documentService.listFolders());
    }

    @PostMapping("/folders")
    @PreAuthorize("@authz.hasPermission('docs:write')")
    public ApiResponse<DocumentService.FolderSummary> createFolder(
            @Valid @RequestBody DocumentService.CreateFolderRequest request) {
        return ApiResponse.ok(documentService.createFolder(request));
    }

    @GetMapping("/files")
    @PreAuthorize("@authz.hasPermission('docs:read')")
    public ApiResponse<List<DocumentService.FileSummary>> listFiles(
            @RequestParam(required = false) UUID folderId) {
        return ApiResponse.ok(documentService.listFiles(folderId));
    }

    @GetMapping("/files/{id}")
    @PreAuthorize("@authz.hasPermission('docs:read')")
    public ApiResponse<DocumentService.FileDetail> getFile(@PathVariable UUID id) {
        return ApiResponse.ok(documentService.getFile(id));
    }

    @PostMapping("/files")
    @PreAuthorize("@authz.hasPermission('docs:write')")
    public ApiResponse<DocumentService.FileDetail> createFile(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @Valid @RequestBody DocumentService.CreateFileRequest request) {
        return ApiResponse.ok(documentService.createFile(principal, request));
    }

    @PatchMapping("/files/{id}")
    @PreAuthorize("@authz.hasPermission('docs:write')")
    public ApiResponse<DocumentService.FileDetail> updateFile(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody DocumentService.UpdateFileRequest request) {
        return ApiResponse.ok(documentService.updateFile(principal, id, request));
    }

    @GetMapping("/signatures")
    @PreAuthorize("@authz.hasPermission('docs:read')")
    public ApiResponse<List<DocumentService.SignatureRequestSummary>> listSignatures(
            @RequestParam(required = false) UUID documentId) {
        return ApiResponse.ok(documentService.listSignatureRequests(documentId));
    }

    @PostMapping("/files/{id}/signature-requests")
    @PreAuthorize("@authz.hasPermission('docs:write')")
    public ApiResponse<DocumentService.SignatureRequestSummary> requestSignature(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody DocumentService.CreateSignatureRequest request) {
        return ApiResponse.ok(documentService.requestSignature(principal, id, request));
    }

    @PostMapping("/signatures/{id}/sign")
    @PreAuthorize("@authz.hasPermission('docs:write')")
    public ApiResponse<DocumentService.SignatureRequestSummary> signDocument(
            @AuthenticationPrincipal ZeniusUserPrincipal principal,
            @PathVariable UUID id) {
        return ApiResponse.ok(documentService.signDocument(principal, id));
    }
}

