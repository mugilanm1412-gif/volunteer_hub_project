package com.volunteerhub.service;

import com.volunteerhub.dto.CertificateDto;
import com.volunteerhub.entity.*;
import com.volunteerhub.exception.ForbiddenException;
import com.volunteerhub.exception.ResourceNotFoundException;
import com.volunteerhub.repository.CertificateRepository;
import com.volunteerhub.repository.NgoProfileRepository;
import com.volunteerhub.repository.UserRepository;
import com.volunteerhub.repository.VolunteerProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service managing automated issuance, retrieval, and PDF document generation
 * for official Certificates of Volunteer Service.
 */
@Service
@Transactional
public class CertificateService {

    private static final Logger log = LoggerFactory.getLogger(CertificateService.class);

    private final CertificateRepository certificateRepository;
    private final com.volunteerhub.repository.AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final PdfReportService pdfReportService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public CertificateService(CertificateRepository certificateRepository,
                              com.volunteerhub.repository.AttendanceRepository attendanceRepository,
                              UserRepository userRepository,
                              VolunteerProfileRepository volunteerProfileRepository,
                              NgoProfileRepository ngoProfileRepository,
                              PdfReportService pdfReportService,
                              AuditLogService auditLogService,
                              NotificationService notificationService) {
        this.certificateRepository = certificateRepository;
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
        this.volunteerProfileRepository = volunteerProfileRepository;
        this.ngoProfileRepository = ngoProfileRepository;
        this.pdfReportService = pdfReportService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    /**
     * Automatically issues a verifiable certificate of service for an attendance record.
     * Guaranteed to be strictly idempotent: if a certificate already exists for this attendance,
     * the existing certificate is returned without creating duplicates.
     *
     * @param attendance the attendance record with verified and approved hours
     * @param reviewer   the NGO owner or administrator approving the hours
     * @return the issued or previously existing Certificate entity
     */
    public synchronized Certificate issueCertificateForAttendance(Attendance attendance, User reviewer) {
        if (attendance == null || attendance.getId() == null) {
            throw new IllegalArgumentException("Attendance and its ID must not be null.");
        }

        // 1. Idempotency Check: if a certificate already exists, return it immediately
        Optional<Certificate> existing = certificateRepository.findByAttendanceId(attendance.getId());
        if (existing.isPresent()) {
            log.info("Certificate already exists for attendance ID {}: {}", attendance.getId(), existing.get().getCertificateNumber());
            return existing.get();
        }

        // 2. Fetch managed attendance with eager associations to guarantee session availability
        Attendance managedAttendance = attendanceRepository.findByIdWithDetails(attendance.getId())
                .orElse(attendance);

        // 3. Preconditions Check: Only PRESENT volunteers with APPROVED hours qualify
        if (managedAttendance.getAttendanceStatus() != AttendanceStatus.PRESENT ||
            managedAttendance.getHoursStatus() != HoursStatus.APPROVED) {
            log.warn("Cannot issue certificate for attendance ID {}: status must be PRESENT and hours APPROVED", managedAttendance.getId());
            return null;
        }

        // 4. Generate safe sequential certificate number: VH-<year>-<6-digit sequence>
        int year = LocalDateTime.now().getYear();
        String certificateNumber = generateNextCertificateNumber(year);

        // 5. Save certificate entity
        Certificate certificate = new Certificate(managedAttendance, certificateNumber);
        Certificate saved = certificateRepository.save(certificate);

        // 6. In-App Notification to Volunteer
        User volunteer = managedAttendance.getApplication().getVolunteer();
        Event event = managedAttendance.getApplication().getEvent();
        String eventTitle = event.getTitle();

        notificationService.createNotification(
                volunteer,
                "CERTIFICATE_ISSUED",
                "Certificate of Volunteer Service Issued",
                String.format("Congratulations! Your certificate for '%s' has been issued: %s.", eventTitle, certificateNumber),
                "CERTIFICATE",
                saved.getId()
        );

        // 7. Audit Trail Logging
        String details = String.format("Issued certificate %s to volunteer %s for event '%s' (%.2f hours).",
                certificateNumber, volunteer.getEmail(), eventTitle, managedAttendance.getHoursWorked());
        auditLogService.logAction(reviewer, "CERTIFICATE_ISSUED", "CERTIFICATE", saved.getId(), details);

        log.info("Successfully issued certificate {} for attendance ID {}", certificateNumber, managedAttendance.getId());
        return saved;
    }

    /**
     * Safely determines the next sequential certificate number for the given year.
     */
    private String generateNextCertificateNumber(int year) {
        String prefix = String.format("VH-%d-%%", year);
        Optional<String> latestOpt = certificateRepository.findLatestCertificateNumberForPrefix(prefix);

        int nextSeq = 1;
        if (latestOpt.isPresent()) {
            String latest = latestOpt.get();
            String[] parts = latest.split("-");
            if (parts.length >= 3) {
                try {
                    nextSeq = Integer.parseInt(parts[2]) + 1;
                } catch (NumberFormatException e) {
                    log.warn("Could not parse numeric sequence from certificate number '{}', defaulting to 1", latest);
                    nextSeq = 1;
                }
            }
        }
        return String.format("VH-%d-%06d", year, nextSeq);
    }

    /**
     * Retrieves all certificates earned by the authenticated volunteer.
     *
     * @param volunteerEmail email of the volunteer
     * @return list of CertificateDto records
     */
    @Transactional(readOnly = true)
    public List<CertificateDto> getVolunteerCertificates(String volunteerEmail) {
        User volunteer = userRepository.findByEmail(volunteerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer not found with email: " + volunteerEmail));

        List<Certificate> certificates = certificateRepository.findByVolunteerIdWithDetails(volunteer.getId());

        return certificates.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Generates the binary PDF document for a certificate, enforcing strict owner volunteer
     * or ADMIN authorization checks.
     *
     * @param certificateId certificate ID
     * @param userEmail     email of the requesting user
     * @param isAdmin       whether the requesting user has ADMIN role
     * @return byte array representing the PDF document
     */
    @Transactional(readOnly = true)
    public byte[] generateCertificatePdf(Long certificateId, String userEmail, boolean isAdmin) {
        Certificate certificate = certificateRepository.findByIdWithDetails(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found with ID: " + certificateId));

        Attendance attendance = certificate.getAttendance();
        User volunteer = attendance.getApplication().getVolunteer();

        // Strict authorization check: Only the owner volunteer or system administrator can download
        if (!isAdmin && !volunteer.getEmail().equalsIgnoreCase(userEmail)) {
            throw new ForbiddenException("Access denied: You are not authorized to download this certificate.");
        }

        Event event = attendance.getApplication().getEvent();
        User ngoUser = event.getNgo();

        String volunteerName = volunteerProfileRepository.findByUserId(volunteer.getId())
                .map(VolunteerProfile::getFullName)
                .orElse(volunteer.getEmail());

        String ngoName = (ngoUser != null)
                ? ngoProfileRepository.findByUserId(ngoUser.getId()).map(NgoProfile::getName).orElse(ngoUser.getEmail())
                : "Host Organization";

        LocalDate eventDate = event.getEventDate();

        return pdfReportService.generateCertificatePdf(
                certificate,
                volunteerName,
                event.getTitle(),
                ngoName,
                attendance.getHoursWorked(),
                eventDate
        );
    }

    /**
     * Retrieves a certificate entity with details for controller download headers.
     */
    @Transactional(readOnly = true)
    public Certificate getCertificateWithDetails(Long certificateId, String userEmail, boolean isAdmin) {
        Certificate certificate = certificateRepository.findByIdWithDetails(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found with ID: " + certificateId));

        User volunteer = certificate.getAttendance().getApplication().getVolunteer();
        if (!isAdmin && !volunteer.getEmail().equalsIgnoreCase(userEmail)) {
            throw new ForbiddenException("Access denied: You are not authorized to view this certificate.");
        }
        return certificate;
    }

    /**
     * Helper mapping entity to CertificateDto.
     */
    private CertificateDto mapToDto(Certificate c) {
        Attendance a = c.getAttendance();
        Application app = a.getApplication();
        User vol = app.getVolunteer();
        Event ev = app.getEvent();
        User ngo = ev.getNgo();

        String volName = volunteerProfileRepository.findByUserId(vol.getId())
                .map(VolunteerProfile::getFullName)
                .orElse(vol.getEmail());

        String ngoName = (ngo != null)
                ? ngoProfileRepository.findByUserId(ngo.getId()).map(NgoProfile::getName).orElse(ngo.getEmail())
                : "Host Organization";

        return new CertificateDto(
                c.getId(),
                a.getId(),
                c.getCertificateNumber(),
                c.getIssuedOn(),
                ev.getId(),
                ev.getTitle(),
                ngoName,
                volName,
                a.getHoursWorked(),
                "/api/certificates/" + c.getId() + "/pdf"
        );
    }
}
