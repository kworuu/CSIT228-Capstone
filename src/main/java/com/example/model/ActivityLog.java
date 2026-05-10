package com.example.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an entry in the audit log of admin actions.
 * Maps to the {@code activity_log} table.
 *
 * <p>Uses polymorphic targeting — {@code targetType} indicates what kind
 * of entity was acted upon (e.g. "evacuee", "transaction"), and
 * {@code targetId} points at it.</p>
 */
public class ActivityLog {

    private Long id;
    private Long userId;
    private String action;
    private String targetType;
    private Long targetId;
    private Map<String, Object> metadata;
    private LocalDateTime timestamp;

    public ActivityLog() {
        this.metadata = new HashMap<>();
    }

    public ActivityLog(Long id, Long userId, String action, String targetType,
                       Long targetId, Map<String, Object> metadata,
                       LocalDateTime timestamp) {
        this.id = id;
        this.userId = userId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.metadata = metadata != null ? metadata : new HashMap<>();
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}