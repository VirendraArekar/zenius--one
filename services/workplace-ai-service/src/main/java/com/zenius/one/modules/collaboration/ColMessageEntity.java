package com.zenius.one.modules.collaboration;

import com.zenius.one.platform.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "col_messages")
public class ColMessageEntity extends TenantScopedEntity {

    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    @Column(name = "sender_name", nullable = false, length = 150)
    private String senderName;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    public UUID getChannelId() {
        return channelId;
    }

    public void setChannelId(UUID channelId) {
        this.channelId = channelId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}

