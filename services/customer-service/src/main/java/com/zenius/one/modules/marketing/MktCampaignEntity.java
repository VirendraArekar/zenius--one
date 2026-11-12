package com.zenius.one.modules.marketing;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mkt_campaigns")
public class MktCampaignEntity extends TenantScopedEntity {

    @Column(name = "segment_id")
    private UUID segmentId;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "channel", nullable = false, length = 32)
    private String channel = "EMAIL";

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "body_content", nullable = false, columnDefinition = "TEXT")
    private String bodyContent;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "DRAFT";

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "sent_count", nullable = false)
    private Integer sentCount = 0;

    @Column(name = "opened_count", nullable = false)
    private Integer openedCount = 0;

    @Column(name = "clicked_count", nullable = false)
    private Integer clickedCount = 0;

    public UUID getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(UUID segmentId) {
        this.segmentId = segmentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBodyContent() {
        return bodyContent;
    }

    public void setBodyContent(String bodyContent) {
        this.bodyContent = bodyContent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public Integer getSentCount() {
        return sentCount;
    }

    public void setSentCount(Integer sentCount) {
        this.sentCount = sentCount;
    }

    public Integer getOpenedCount() {
        return openedCount;
    }

    public void setOpenedCount(Integer openedCount) {
        this.openedCount = openedCount;
    }

    public Integer getClickedCount() {
        return clickedCount;
    }

    public void setClickedCount(Integer clickedCount) {
        this.clickedCount = clickedCount;
    }
}

