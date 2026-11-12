package com.zenius.one.modules.documents;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "doc_signature_requests")
public class DocSignatureRequestEntity extends TenantScopedEntity {

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "signer_name", nullable = false, length = 150)
    private String signerName;

    @Column(name = "signer_email", nullable = false, length = 180)
    private String signerEmail;

    @Column(nullable = false, length = 32)
    private String status = "PENDING";

    @Column(name = "signature_hash", length = 128)
    private String signatureHash;

    @Column(name = "signed_at")
    private Instant signedAt;

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getSignerName() {
        return signerName;
    }

    public void setSignerName(String signerName) {
        this.signerName = signerName;
    }

    public String getSignerEmail() {
        return signerEmail;
    }

    public void setSignerEmail(String signerEmail) {
        this.signerEmail = signerEmail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSignatureHash() {
        return signatureHash;
    }

    public void setSignatureHash(String signatureHash) {
        this.signatureHash = signatureHash;
    }

    public Instant getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(Instant signedAt) {
        this.signedAt = signedAt;
    }
}

