package com.volunteerhub.dto;

import java.time.Instant;

/**
 * DTO representing an in-app notification.
 */
public class NotificationDto {

    private Long id;
    private String type;
    private String title;
    private String message;
    private boolean read;
    private String relatedEntityType;
    private Long relatedEntityId;
    private Instant createdAt;

    public NotificationDto() {
    }

    public NotificationDto(Long id, String type, String title, String message, boolean read,
                           String relatedEntityType, Long relatedEntityId, Instant createdAt) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.read = read;
        this.relatedEntityType = relatedEntityType;
        this.relatedEntityId = relatedEntityId;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getRelatedEntityType() {
        return relatedEntityType;
    }

    public void setRelatedEntityType(String relatedEntityType) {
        this.relatedEntityType = relatedEntityType;
    }

    public Long getRelatedEntityId() {
        return relatedEntityId;
    }

    public void setRelatedEntityId(Long relatedEntityId) {
        this.relatedEntityId = relatedEntityId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
