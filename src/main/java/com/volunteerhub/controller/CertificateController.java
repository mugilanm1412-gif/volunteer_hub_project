package com.volunteerhub.controller;

import com.volunteerhub.dto.CertificateDto;
import com.volunteerhub.entity.Certificate;
import com.volunteerhub.service.CertificateService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for retrieving issued volunteer certificates and downloading official PDF credentials.
 */
@RestController
@RequestMapping("/api")
public class CertificateController {

    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    /**
     * Retrieves all certificates issued to the authenticated volunteer.
     */
    @GetMapping("/volunteers/me/certificates")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<List<CertificateDto>> getMyCertificates(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<CertificateDto> certificates = certificateService.getVolunteerCertificates(userDetails.getUsername());
        return ResponseEntity.ok(certificates);
    }

    /**
     * Generates and downloads an official OpenPDF Certificate of Volunteer Service document.
     * Restricted to the owner volunteer or platform administrators.
     */
    @GetMapping("/certificates/{id}/pdf")
    @PreAuthorize("hasAnyRole('VOLUNTEER', 'ADMIN')")
    public ResponseEntity<byte[]> downloadCertificatePdf(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        Certificate certificate = certificateService.getCertificateWithDetails(id, userDetails.getUsername(), isAdmin);
        byte[] pdfBytes = certificateService.generateCertificatePdf(id, userDetails.getUsername(), isAdmin);

        String filename = String.format("certificate-%s.pdf", certificate.getCertificateNumber());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
