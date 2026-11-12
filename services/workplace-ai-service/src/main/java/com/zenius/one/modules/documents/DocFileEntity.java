package com.zenius.one.modules.documents;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "doc_files")
public class DocFileEntity extends TenantScopedEntity {

    @Column(name = "folder_id")
    private UUID folderId;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(name = "doc_type", nullable = false, length = 32)
    private String docType = "WORD";

    @Column(name = "content_body", nullable = false, columnDefinition = "TEXT")
    private String contentBody = "";

    @Column(name = "current_version", nullable = false)
    private int currentVersion = 1;

    @Column(name = "retention_policy", length = 64)
    private String retentionPolicy = "STANDARD_7Y";

    public UUID getFolderId() {
        return folderId;
    }

    public void setFolderId(UUID folderId) {
        this.folderId = folderId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getContentBody() {
        return contentBody;
    }

    public void setContentBody(String contentBody) {
        this.contentBody = contentBody;
    }

    public int getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(int currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getRetentionPolicy() {
        return retentionPolicy;
    }

    public void setRetentionPolicy(String retentionPolicy) {
        this.retentionPolicy = retentionPolicy;
    }
}

