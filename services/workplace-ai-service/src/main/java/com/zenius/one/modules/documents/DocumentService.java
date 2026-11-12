package com.zenius.one.modules.documents;

import com.zenius.one.modules.identity.security.TokenHashing;
import com.zenius.one.modules.identity.security.ZeniusUserPrincipal;
import com.zenius.one.modules.itsecurity.AuditLogService;
import com.zenius.one.platform.common.ResourceNotFoundException;
import com.zenius.one.platform.common.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentService {

    private static final Set<String> VALID_DOC_TYPES = Set.of("WORD", "SHEET", "SLIDE", "PDF", "NOTE");

    private final DocFolderRepository folderRepository;
    private final DocFileRepository fileRepository;
    private final DocRevisionRepository revisionRepository;
    private final DocSignatureRequestRepository signatureRequestRepository;
    private final AuditLogService auditLogService;

    public DocumentService(
            DocFolderRepository folderRepository,
            DocFileRepository fileRepository,
            DocRevisionRepository revisionRepository,
            DocSignatureRequestRepository signatureRequestRepository,
            AuditLogService auditLogService) {
        this.folderRepository = folderRepository;
        this.fileRepository = fileRepository;
        this.revisionRepository = revisionRepository;
        this.signatureRequestRepository = signatureRequestRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public List<FolderSummary> listFolders() {
        UUID orgId = TenantContext.requireOrganizationId();
        List<DocFolderEntity> folders = folderRepository.findByOrganizationIdOrderByNameAsc(orgId);
        if (folders.isEmpty()) {
            DocFolderEntity root = new DocFolderEntity();
            root.setOrganizationId(orgId);
            root.setName("Shared Drive");
            root.setShared(true);
            folderRepository.save(root);
            folders = folderRepository.findByOrganizationIdOrderByNameAsc(orgId);
        }
        return folders.stream().map(FolderSummary::from).toList();
    }

    @Transactional
    public FolderSummary createFolder(@Valid CreateFolderRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        if (request.parentFolderId() != null) {
            folderRepository
                    .findByIdAndOrganizationId(request.parentFolderId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent folder not found"));
        }
        DocFolderEntity folder = new DocFolderEntity();
        folder.setOrganizationId(orgId);
        folder.setParentFolderId(request.parentFolderId());
        folder.setName(request.name().trim());
        folder.setShared(request.shared() == null || request.shared());
        return FolderSummary.from(folderRepository.save(folder));
    }

    @Transactional(readOnly = true)
    public List<FileSummary> listFiles(UUID folderId) {
        UUID orgId = TenantContext.requireOrganizationId();
        List<DocFileEntity> files = folderId == null
                ? fileRepository.findByOrganizationIdOrderByUpdatedAtDesc(orgId)
                : fileRepository.findByOrganizationIdAndFolderIdOrderByUpdatedAtDesc(orgId, folderId);
        return files.stream().map(FileSummary::from).toList();
    }

    @Transactional(readOnly = true)
    public FileDetail getFile(UUID fileId) {
        UUID orgId = TenantContext.requireOrganizationId();
        DocFileEntity file = fileRepository
                .findByIdAndOrganizationId(fileId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        List<RevisionSummary> revisions = revisionRepository
                .findByOrganizationIdAndDocumentIdOrderByRevisionNumberDesc(orgId, fileId)
                .stream()
                .map(RevisionSummary::from)
                .toList();
        List<SignatureRequestSummary> signatures = signatureRequestRepository
                .findByOrganizationIdAndDocumentIdOrderByCreatedAtDesc(orgId, fileId)
                .stream()
                .map(SignatureRequestSummary::from)
                .toList();
        return new FileDetail(FileSummary.from(file), file.getContentBody(), revisions, signatures);
    }

    @Transactional
    public FileDetail createFile(ZeniusUserPrincipal principal, @Valid CreateFileRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        UUID folderId = resolveFolderId(orgId, request.folderId());
        String docType = normalizeDocType(request.docType());
        String body = request.contentBody() == null ? "" : request.contentBody();

        DocFileEntity file = new DocFileEntity();
        file.setOrganizationId(orgId);
        file.setFolderId(folderId);
        file.setTitle(request.title().trim());
        file.setDocType(docType);
        file.setContentBody(body);
        file.setCurrentVersion(1);
        file.setRetentionPolicy(
                request.retentionPolicy() == null || request.retentionPolicy().isBlank()
                        ? "STANDARD_7Y"
                        : request.retentionPolicy().trim());
        DocFileEntity saved = fileRepository.save(file);

        DocRevisionEntity revision = new DocRevisionEntity();
        revision.setOrganizationId(orgId);
        revision.setDocumentId(saved.getId());
        revision.setRevisionNumber(1);
        revision.setEditorName(principal.getFullName());
        revision.setContentSnapshot(body);
        revisionRepository.save(revision);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "DOC_CREATED",
                "DOC_FILE",
                saved.getId().toString(),
                "Created " + docType + " document " + saved.getTitle(),
                "SUCCESS");

        return getFile(saved.getId());
    }

    @Transactional
    public FileDetail updateFile(ZeniusUserPrincipal principal, UUID fileId, @Valid UpdateFileRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        DocFileEntity file = fileRepository
                .findByIdAndOrganizationId(fileId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        if (request.title() != null && !request.title().isBlank()) {
            file.setTitle(request.title().trim());
        }
        if (request.retentionPolicy() != null && !request.retentionPolicy().isBlank()) {
            file.setRetentionPolicy(request.retentionPolicy().trim());
        }
        String newBody = request.contentBody() == null ? file.getContentBody() : request.contentBody();
        file.setContentBody(newBody);
        int nextVersion = file.getCurrentVersion() + 1;
        file.setCurrentVersion(nextVersion);
        DocFileEntity saved = fileRepository.save(file);

        DocRevisionEntity revision = new DocRevisionEntity();
        revision.setOrganizationId(orgId);
        revision.setDocumentId(saved.getId());
        revision.setRevisionNumber(nextVersion);
        revision.setEditorName(principal.getFullName());
        revision.setContentSnapshot(newBody);
        revisionRepository.save(revision);

        return getFile(saved.getId());
    }

    @Transactional(readOnly = true)
    public List<SignatureRequestSummary> listSignatureRequests(UUID documentId) {
        UUID orgId = TenantContext.requireOrganizationId();
        List<DocSignatureRequestEntity> list = documentId == null
                ? signatureRequestRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId)
                : signatureRequestRepository.findByOrganizationIdAndDocumentIdOrderByCreatedAtDesc(orgId, documentId);
        return list.stream().map(SignatureRequestSummary::from).toList();
    }

    @Transactional
    public SignatureRequestSummary requestSignature(
            ZeniusUserPrincipal principal, UUID fileId, @Valid CreateSignatureRequest request) {
        UUID orgId = TenantContext.requireOrganizationId();
        DocFileEntity file = fileRepository
                .findByIdAndOrganizationId(fileId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        DocSignatureRequestEntity sig = new DocSignatureRequestEntity();
        sig.setOrganizationId(orgId);
        sig.setDocumentId(file.getId());
        sig.setSignerName(request.signerName().trim());
        sig.setSignerEmail(request.signerEmail().trim().toLowerCase());
        sig.setStatus("PENDING");
        DocSignatureRequestEntity saved = signatureRequestRepository.save(sig);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "SIGNATURE_REQUESTED",
                "DOC_SIGNATURE",
                saved.getId().toString(),
                "Requested e-signature from " + saved.getSignerEmail() + " on " + file.getTitle(),
                "SUCCESS");

        return SignatureRequestSummary.from(saved);
    }

    @Transactional
    public SignatureRequestSummary signDocument(ZeniusUserPrincipal principal, UUID signatureRequestId) {
        UUID orgId = TenantContext.requireOrganizationId();
        DocSignatureRequestEntity sig = signatureRequestRepository
                .findByIdAndOrganizationId(signatureRequestId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Signature request not found"));

        if ("SIGNED".equalsIgnoreCase(sig.getStatus())) {
            throw new IllegalArgumentException("Signature request is already signed");
        }

        DocFileEntity file = fileRepository
                .findByIdAndOrganizationId(sig.getDocumentId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        Instant now = Instant.now();
        String payload = orgId + ":" + file.getId() + ":v" + file.getCurrentVersion() + ":"
                + sig.getSignerEmail() + ":" + now.toString() + ":" + file.getContentBody();
        String hash = TokenHashing.sha256Hex(payload);

        sig.setStatus("SIGNED");
        sig.setSignatureHash(hash);
        sig.setSignedAt(now);
        DocSignatureRequestEntity saved = signatureRequestRepository.save(sig);

        auditLogService.record(
                orgId,
                principal.getUserId(),
                principal.getEmail(),
                "DOC_SIGNED",
                "DOC_SIGNATURE",
                saved.getId().toString(),
                "Digitally signed document " + file.getTitle() + " (hash=" + hash.substring(0, 12) + "...)",
                "SUCCESS");

        return SignatureRequestSummary.from(saved);
    }

    private UUID resolveFolderId(UUID orgId, UUID requestedFolderId) {
        if (requestedFolderId != null) {
            return folderRepository
                    .findByIdAndOrganizationId(requestedFolderId, orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Folder not found"))
                    .getId();
        }
        return folderRepository
                .findFirstByOrganizationIdAndParentFolderIdIsNullOrderByCreatedAtAsc(orgId)
                .map(DocFolderEntity::getId)
                .orElseGet(() -> {
                    DocFolderEntity root = new DocFolderEntity();
                    root.setOrganizationId(orgId);
                    root.setName("Shared Drive");
                    root.setShared(true);
                    return folderRepository.save(root).getId();
                });
    }

    private String normalizeDocType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "WORD";
        }
        String normalized = raw.trim().toUpperCase();
        if (!VALID_DOC_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported docType: " + raw);
        }
        return normalized;
    }

    public record CreateFolderRequest(UUID parentFolderId, @NotBlank @Size(max = 140) String name, Boolean shared) {}

    public record CreateFileRequest(
            UUID folderId,
            @NotBlank @Size(max = 180) String title,
            String docType,
            String contentBody,
            @Size(max = 64) String retentionPolicy) {}

    public record UpdateFileRequest(@Size(max = 180) String title, String contentBody, @Size(max = 64) String retentionPolicy) {}

    public record CreateSignatureRequest(
            @NotBlank @Size(max = 150) String signerName,
            @NotBlank @Email @Size(max = 180) String signerEmail) {}

    public record FolderSummary(UUID id, UUID parentFolderId, String name, boolean shared, Instant createdAt) {
        public static FolderSummary from(DocFolderEntity entity) {
            return new FolderSummary(
                    entity.getId(), entity.getParentFolderId(), entity.getName(), entity.isShared(), entity.getCreatedAt());
        }
    }

    public record FileSummary(
            UUID id,
            UUID folderId,
            String title,
            String docType,
            int currentVersion,
            String retentionPolicy,
            Instant updatedAt) {
        public static FileSummary from(DocFileEntity entity) {
            return new FileSummary(
                    entity.getId(),
                    entity.getFolderId(),
                    entity.getTitle(),
                    entity.getDocType(),
                    entity.getCurrentVersion(),
                    entity.getRetentionPolicy(),
                    entity.getUpdatedAt());
        }
    }

    public record RevisionSummary(
            UUID id, int revisionNumber, String editorName, String contentSnapshot, Instant createdAt) {
        public static RevisionSummary from(DocRevisionEntity entity) {
            return new RevisionSummary(
                    entity.getId(),
                    entity.getRevisionNumber(),
                    entity.getEditorName(),
                    entity.getContentSnapshot(),
                    entity.getCreatedAt());
        }
    }

    public record SignatureRequestSummary(
            UUID id,
            UUID documentId,
            String signerName,
            String signerEmail,
            String status,
            String signatureHash,
            Instant signedAt,
            Instant createdAt) {
        public static SignatureRequestSummary from(DocSignatureRequestEntity entity) {
            return new SignatureRequestSummary(
                    entity.getId(),
                    entity.getDocumentId(),
                    entity.getSignerName(),
                    entity.getSignerEmail(),
                    entity.getStatus(),
                    entity.getSignatureHash(),
                    entity.getSignedAt(),
                    entity.getCreatedAt());
        }
    }

    public record FileDetail(
            FileSummary file,
            String contentBody,
            List<RevisionSummary> revisions,
            List<SignatureRequestSummary> signatures) {}
}

