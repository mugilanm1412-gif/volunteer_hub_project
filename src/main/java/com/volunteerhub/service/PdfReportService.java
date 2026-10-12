package com.volunteerhub.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.volunteerhub.dto.VolunteerPerformanceDto;
import com.volunteerhub.entity.Attendance;
import com.volunteerhub.entity.VolunteerProfile;
import com.volunteerhub.entity.VolunteerRating;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for generating styled, professional OpenPDF performance reports and transcripts.
 */
@Service
public class PdfReportService {

    private static final Color COLOR_PRIMARY = new Color(15, 118, 110);    // Emerald / Teal
    private static final Color COLOR_DARK = new Color(30, 41, 59);          // Slate 800
    private static final Color COLOR_MUTED = new Color(100, 116, 139);      // Slate 500
    private static final Color COLOR_BG_LIGHT = new Color(248, 250, 252);   // Slate 50
    private static final Color COLOR_BORDER = new Color(226, 232, 240);     // Slate 200
    private static final Color COLOR_GOLD = new Color(217, 119, 6);         // Amber 600

    public byte[] generatePerformancePdf(VolunteerProfile profile,
                                         VolunteerPerformanceDto performance,
                                         List<Attendance> attendances,
                                         List<VolunteerRating> ratings) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Header & Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, COLOR_PRIMARY);
            Paragraph title = new Paragraph("VOLUNTEER HUB PERFORMANCE REPORT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, COLOR_MUTED);
            Paragraph subTitle = new Paragraph("Official Service Transcript & Verification Document", subTitleFont);
            subTitle.setAlignment(Element.ALIGN_CENTER);
            subTitle.setSpacingAfter(15f);
            document.add(subTitle);

            // 2. Volunteer Biographical Metadata Box
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{1.5f, 1.5f});
            infoTable.setSpacingAfter(15f);

            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOR_DARK);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 9, COLOR_DARK);

            addInfoCell(infoTable, "Volunteer Name:", profile.getFullName(), labelFont, valueFont);
            addInfoCell(infoTable, "Email Address:", profile.getUser().getEmail(), labelFont, valueFont);
            addInfoCell(infoTable, "Report Date:", LocalDate.now().format(DateTimeFormatter.ISO_DATE), labelFont, valueFont);
            addInfoCell(infoTable, "Leaderboard Status:", performance.isShowOnLeaderboard() ? "Publicly Visible" : "Opted Out (Private)", labelFont, valueFont);

            document.add(infoTable);

            // 3. Performance Summary Table
            Paragraph metricsHeader = new Paragraph("PERFORMANCE SUMMARY & METRICS", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_PRIMARY));
            metricsHeader.setSpacingAfter(6f);
            document.add(metricsHeader);

            PdfPTable metricsTable = new PdfPTable(4);
            metricsTable.setWidthPercentage(100);
            metricsTable.setWidths(new float[]{1f, 1f, 1f, 1f});
            metricsTable.setSpacingAfter(15f);

            addHeaderCell(metricsTable, "Verified Hours");
            addHeaderCell(metricsTable, "Attended Events");
            addHeaderCell(metricsTable, "Attendance Rate");
            addHeaderCell(metricsTable, "Average Rating");

            addMetricCell(metricsTable, performance.getTotalVerifiedHours() != null ? performance.getTotalVerifiedHours().toPlainString() + " hrs" : "0 hrs");
            addMetricCell(metricsTable, performance.getCompletedEvents() + " events");
            addMetricCell(metricsTable, String.format("%.1f%%", performance.getAttendanceRate()));
            addMetricCell(metricsTable, performance.getAverageRating() != null ? String.format("%.2f / 5.0", performance.getAverageRating()) : "Unrated");

            addHeaderCell(metricsTable, "Hours Score");
            addHeaderCell(metricsTable, "Attendance Score");
            addHeaderCell(metricsTable, "Composite Score");
            addHeaderCell(metricsTable, "Awarded Badge");

            addMetricCell(metricsTable, String.format("%.1f", performance.getHoursScore()));
            addMetricCell(metricsTable, String.format("%.1f", performance.getAttendanceScore()));
            addMetricCell(metricsTable, String.format("%.2f / 100", performance.getPerformanceScore()));

            Font badgeFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_GOLD);
            PdfPCell badgeCell = new PdfPCell(new Phrase(performance.getBadge(), badgeFont));
            badgeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            badgeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            badgeCell.setPadding(6f);
            badgeCell.setBackgroundColor(COLOR_BG_LIGHT);
            badgeCell.setBorderColor(COLOR_BORDER);
            metricsTable.addCell(badgeCell);

            document.add(metricsTable);

            // 4. Completed Service Records
            Paragraph historyHeader = new Paragraph("SERVICE & ATTENDANCE HISTORY", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_PRIMARY));
            historyHeader.setSpacingAfter(6f);
            document.add(historyHeader);

            PdfPTable historyTable = new PdfPTable(5);
            historyTable.setWidthPercentage(100);
            historyTable.setWidths(new float[]{1.1f, 2.2f, 1.8f, 1f, 1f});
            historyTable.setSpacingAfter(15f);

            addHeaderCell(historyTable, "Date");
            addHeaderCell(historyTable, "Event");
            addHeaderCell(historyTable, "Host NGO");
            addHeaderCell(historyTable, "Status");
            addHeaderCell(historyTable, "Hours");

            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_DARK);

            if (attendances == null || attendances.isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No attendance records found.", regularFont));
                emptyCell.setColspan(5);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                emptyCell.setPadding(8f);
                historyTable.addCell(emptyCell);
            } else {
                for (Attendance att : attendances) {
                    String date = att.getApplication().getEvent().getEventDate().toString();
                    String eventTitle = att.getApplication().getEvent().getTitle();
                    String ngoName = att.getApplication().getEvent().getNgo() != null ? att.getApplication().getEvent().getNgo().getEmail() : "NGO";
                    String status = att.getAttendanceStatus() != null ? att.getAttendanceStatus().name() : "N/A";
                    String hours = (att.getHoursWorked() != null ? att.getHoursWorked().toPlainString() : "0") + " (" + att.getHoursStatus() + ")";

                    addTextCell(historyTable, date, regularFont);
                    addTextCell(historyTable, eventTitle, regularFont);
                    addTextCell(historyTable, ngoName, regularFont);
                    addTextCell(historyTable, status, regularFont);
                    addTextCell(historyTable, hours, regularFont);
                }
            }
            document.add(historyTable);

            // 5. Ratings & Feedback
            if (ratings != null && !ratings.isEmpty()) {
                Paragraph ratingsHeader = new Paragraph("NGO EVALUATIONS & FEEDBACK", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_PRIMARY));
                ratingsHeader.setSpacingAfter(6f);
                document.add(ratingsHeader);

                PdfPTable ratingTable = new PdfPTable(4);
                ratingTable.setWidthPercentage(100);
                ratingTable.setWidths(new float[]{1.5f, 1.2f, 1.2f, 3.1f});
                ratingTable.setSpacingAfter(15f);

                addHeaderCell(ratingTable, "Event");
                addHeaderCell(ratingTable, "Rated By");
                addHeaderCell(ratingTable, "Scores (P/T/E)");
                addHeaderCell(ratingTable, "Feedback");

                for (VolunteerRating r : ratings) {
                    String eventTitle = r.getAttendance().getApplication().getEvent().getTitle();
                    String ratedBy = r.getRatedBy() != null ? r.getRatedBy().getEmail() : "NGO";
                    String scores = r.getPunctuality() + " / " + r.getTeamwork() + " / " + r.getEffort();
                    String comment = r.getComment() != null ? r.getComment() : "-";

                    addTextCell(ratingTable, eventTitle, regularFont);
                    addTextCell(ratingTable, ratedBy, regularFont);
                    addTextCell(ratingTable, scores, regularFont);
                    addTextCell(ratingTable, comment, regularFont);
                }
                document.add(ratingTable);
            }

            // 6. Verification Footer
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, COLOR_MUTED);
            Paragraph footer = new Paragraph("This document is an electronically verified record generated by the Volunteer Hub Platform.", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(10f);
            document.add(footer);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF performance report: " + e.getMessage(), e);
        }
    }

    private void addInfoCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        Phrase phrase = new Phrase();
        phrase.add(new Chunk(label + " ", labelFont));
        phrase.add(new Chunk(value != null ? value : "", valueFont));
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorderColor(COLOR_BORDER);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, headerFont));
        cell.setBackgroundColor(COLOR_PRIMARY);
        cell.setBorderColor(COLOR_BORDER);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addMetricCell(PdfPTable table, String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOR_DARK);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(COLOR_BG_LIGHT);
        cell.setBorderColor(COLOR_BORDER);
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addTextCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setBorderColor(COLOR_BORDER);
        cell.setPadding(4f);
        table.addCell(cell);
    }
}
