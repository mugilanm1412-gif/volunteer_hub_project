package com.volunteerhub.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing an official, verifiable certificate of volunteer service.
 * Automatically issued upon verified approval of attendance hours.
 */
@Entity
@Table(name = "certificates",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_certificate_attendance", columnNames = "attendance_id"),
           @UniqueConstraint(name = "uk_certificate_number", columnNames = "certificate_number")
       })
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id", nullable = false, unique = true)
    private Attendance attendance;

    @Column(name = "certificate_number", nullable = false, unique = true, length = 50)
    private String certificateNumber;

    @Column(name = "issued_on", nullable = false, updatable = false)
    private LocalDateTime issuedOn = LocalDateTime.now();

    public Certificate() {
    }

    public Certificate(Attendance attendance, String certificateNumber) {
        this.attendance = attendance;
        this.certificateNumber = certificateNumber;
        this.issuedOn = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Attendance getAttendance() {
        return attendance;
    }

    public void setAttendance(Attendance attendance) {
        this.attendance = attendance;
    }

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public void setCertificateNumber(String certificateNumber) {
        this.certificateNumber = certificateNumber;
    }

    public LocalDateTime getIssuedOn() {
        return issuedOn;
    }

    public void setIssuedOn(LocalDateTime issuedOn) {
        this.issuedOn = issuedOn;
    }
}
