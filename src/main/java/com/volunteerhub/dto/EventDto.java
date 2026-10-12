package com.volunteerhub.dto;

import com.volunteerhub.entity.EventStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Data Transfer Object representing volunteering event details.
 */
public class EventDto {

    private Long id;
    private Long ngoId;
    private String ngoName;
    private String ngoContactPerson;
    private String ngoContactPhone;
    private String title;
    private String description;
    private String category;
    private String location;
    private LocalDate eventDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String requiredSkills;
    private Integer capacity;
    private Integer approvedCount;
    private Integer remainingSlots;
    private EventStatus status;
    private String removedReason;
    private String userApplicationStatus;
    private LocalDateTime createdAt;

    public EventDto() {
    }

    public EventDto(Long id, Long ngoId, String ngoName, String ngoContactPerson, String ngoContactPhone,
                    String title, String description, String category, String location,
                    LocalDate eventDate, LocalTime startTime, LocalTime endTime,
                    String requiredSkills, Integer capacity, Integer approvedCount, Integer remainingSlots,
                    EventStatus status, String removedReason, String userApplicationStatus, LocalDateTime createdAt) {
        this.id = id;
        this.ngoId = ngoId;
        this.ngoName = ngoName;
        this.ngoContactPerson = ngoContactPerson;
        this.ngoContactPhone = ngoContactPhone;
        this.title = title;
        this.description = description;
        this.category = category;
        this.location = location;
        this.eventDate = eventDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.requiredSkills = requiredSkills;
        this.capacity = capacity;
        this.approvedCount = approvedCount;
        this.remainingSlots = remainingSlots;
        this.status = status;
        this.removedReason = removedReason;
        this.userApplicationStatus = userApplicationStatus;
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

    public String getNgoContactPerson() {
        return ngoContactPerson;
    }

    public void setNgoContactPerson(String ngoContactPerson) {
        this.ngoContactPerson = ngoContactPerson;
    }

    public String getNgoContactPhone() {
        return ngoContactPhone;
    }

    public void setNgoContactPhone(String ngoContactPhone) {
        this.ngoContactPhone = ngoContactPhone;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(String requiredSkills) {
        this.requiredSkills = requiredSkills;
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

    public Integer getRemainingSlots() {
        return remainingSlots;
    }

    public void setRemainingSlots(Integer remainingSlots) {
        this.remainingSlots = remainingSlots;
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

    public String getUserApplicationStatus() {
        return userApplicationStatus;
    }

    public void setUserApplicationStatus(String userApplicationStatus) {
        this.userApplicationStatus = userApplicationStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
