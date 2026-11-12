package com.zenius.one.modules.creator;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "crt_records")
public class CrtRecordEntity extends TenantScopedEntity {

    @Column(name = "form_id", nullable = false)
    private UUID formId;

    @Column(name = "record_number", nullable = false, length = 40)
    private String recordNumber;

    @Column(name = "data_json", nullable = false, columnDefinition = "TEXT")
    private String dataJson = "{}";

    @Column(name = "submitted_by", nullable = false, length = 160)
    private String submittedBy;

    public UUID getFormId() {
        return formId;
    }

    public void setFormId(UUID formId) {
        this.formId = formId;
    }

    public String getRecordNumber() {
        return recordNumber;
    }

    public void setRecordNumber(String recordNumber) {
        this.recordNumber = recordNumber;
    }

    public String getDataJson() {
        return dataJson;
    }

    public void setDataJson(String dataJson) {
        this.dataJson = dataJson;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }
}

