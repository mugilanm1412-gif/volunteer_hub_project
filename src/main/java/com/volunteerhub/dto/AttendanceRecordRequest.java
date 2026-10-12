package com.volunteerhub.dto;

import com.volunteerhub.entity.AttendanceStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Payload for marking individual attendance within a batch submission.
 */
public class AttendanceRecordRequest {

    @NotNull(message = "Application ID is required")
    private Long applicationId;

    @NotNull(message = "Attendance status is required")
    private AttendanceStatus attendanceStatus;

    @DecimalMin(value = "0.0", message = "Hours worked cannot be negative")
    private BigDecimal hoursWorked;

    public AttendanceRecordRequest() {
    }

    public AttendanceRecordRequest(Long applicationId, AttendanceStatus attendanceStatus, BigDecimal hoursWorked) {
        this.applicationId = applicationId;
        this.attendanceStatus = attendanceStatus;
        this.hoursWorked = hoursWorked;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public AttendanceStatus getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(AttendanceStatus attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    public BigDecimal getHoursWorked() {
        return hoursWorked;
    }

    public void setHoursWorked(BigDecimal hoursWorked) {
        this.hoursWorked = hoursWorked;
    }
}
