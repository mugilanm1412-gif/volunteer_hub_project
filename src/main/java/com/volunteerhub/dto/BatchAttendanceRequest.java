package com.volunteerhub.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Batch request payload for marking attendance on multiple event registrations at once.
 */
public class BatchAttendanceRequest {

    @NotEmpty(message = "Attendance records list cannot be empty")
    private List<@Valid AttendanceRecordRequest> records;

    public BatchAttendanceRequest() {
    }

    public BatchAttendanceRequest(List<AttendanceRecordRequest> records) {
        this.records = records;
    }

    public List<AttendanceRecordRequest> getRecords() {
        return records;
    }

    public void setRecords(List<AttendanceRecordRequest> records) {
        this.records = records;
    }
}
