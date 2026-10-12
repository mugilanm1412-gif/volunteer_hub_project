package com.volunteerhub.dto;

import com.volunteerhub.entity.EventStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Representation of an event for administrative moderation and governance views.
 */
public class AdminEventItemDto {

    private Long id;
    private Long ngoId;
    private String ngoName;
    private String title;
    private String category;
    private String location;
    private LocalDate eventDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer capacity;
    private Integer approvedCount;
    private EventStatus status;
    private String removedReason;
    private LocalDateTime createdAt;

    public AdminEventItemDto() {
    }

    public AdminEventItemDto(Long id, Long ngoId, String ngoName, String title, String category,
                             String location, LocalDate eventDate, LocalTime startTime, LocalTime endTime,
                             Integer capacity, Integer approvedCount, EventStatus status,
                             String removedReason, LocalDateTime createdAt) {
        this.id = id;
        this.ngoId = ngoId;
        this.ngoName = ngoName;
        this.title = title;
        this.category = category;
        this.location = location;
        this.eventDate = eventDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.capacity = capacity;
        this.approvedCount = approvedCount;
        this.status = status;
        this.removedReason = removedReason;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNgoId() {
        return ngoId;
    }

    public void setNgoId(Long ngoId) {
        this.ngoId = ngoId;
    }

    public String getNgoName() {
        return ngoName;
    }

    public void setNgoName(String ngoName) {
        this.ngoName = ngoName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getApprovedCount() {
        return approvedCount;
    }

    public void setApprovedCount(Integer approvedCount) {
        this.approvedCount = approvedCount;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public String getRemovedReason() {
        return removedReason;
    }

    public void setRemovedReason(String removedReason) {
        this.removedReason = removedReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
