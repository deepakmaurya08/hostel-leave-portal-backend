package com.akgec.hostel.pdf;

import com.akgec.hostel.model.entity.GatePass;
import com.akgec.hostel.model.entity.LeaveRequest;
import com.akgec.hostel.model.entity.Student;
import com.akgec.hostel.qr.QrCodeGenerator;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeavePdfGenerator {

    private final QrCodeGenerator qrCodeGenerator;

    @Value("${app.file.pdf-dir}")
    private String pdfDir;

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final Color NAVY      = new Color(10, 22, 40);
    private static final Color INDIGO    = new Color(79, 70, 229);
    private static final Color LIGHT_BG  = new Color(248, 250, 252);
    private static final Color BORDER    = new Color(226, 232, 240);
    private static final Color EMERALD   = new Color(5, 150, 105);
    private static final Color TEXT_MAIN = new Color(15, 23, 42);
    private static final Color TEXT_MUTE = new Color(100, 116, 139);

    public String generateLeavePdf(LeaveRequest leave, GatePass gatePass) throws Exception {
        Path dir = Paths.get(pdfDir);
        Files.createDirectories(dir);
        String fileName = "leave_pass_" + leave.getLeavePassNumber().replace("-", "_") + ".pdf";
        Path filePath   = dir.resolve(fileName);

        Document doc = new Document(PageSize.A4, 45, 45, 45, 40);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(filePath.toFile()));
        doc.open();

        addHeader(doc, leave);
        addStudentSection(doc, leave, gatePass);
        addLeaveDetailsSection(doc, leave);
        addApprovalSection(doc, leave);
        addWardenRemarksSection(doc, leave);
        addFooter(doc, leave);

        doc.close();
        log.info("PDF generated: {}", filePath);
        return filePath.toString();
    }

    // ── HEADER ────────────────────────────────────────────────────────────────
    private void addHeader(Document doc, LeaveRequest leave) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);

        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(NAVY);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(16);

        Font college = new Font(Font.HELVETICA, 15, Font.BOLD, Color.WHITE);
        Font sub     = new Font(Font.HELVETICA, 9,  Font.NORMAL, new Color(165, 180, 252));
        Font passNo  = new Font(Font.HELVETICA, 10, Font.BOLD,   new Color(250, 204, 21));

        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.add(new Chunk("AJAY KUMAR GARG ENGINEERING COLLEGE, GHAZIABAD\n", college));
        p.add(new Chunk("Hostel Leave Management Portal\n", sub));
        p.add(new Chunk("APPROVED LEAVE PASS — " + leave.getLeavePassNumber(), passNo));
        c.addElement(p);
        t.addCell(c);

        // Green status bar
        PdfPCell status = new PdfPCell(new Phrase("✓  FULLY APPROVED",
                new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE)));
        status.setBackgroundColor(EMERALD);
        status.setBorder(Rectangle.NO_BORDER);
        status.setPadding(8);
        status.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(status);

        doc.add(t);
        doc.add(Chunk.NEWLINE);
    }

    // ── STUDENT INFO + PHOTO + QR ─────────────────────────────────────────────
    private void addStudentSection(Document doc, LeaveRequest leave, GatePass gatePass) throws Exception {
        Student s = leave.getStudent();

        PdfPTable outer = new PdfPTable(new float[]{1f, 3f});
        outer.setWidthPercentage(100);
        outer.setSpacingAfter(10);

        // Photo cell
        PdfPCell photoCell = new PdfPCell();
        photoCell.setBorder(Rectangle.BOX);
        photoCell.setBorderColor(BORDER);
        photoCell.setPadding(8);
        photoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        photoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        String photoPath = null;
        if (s.getUser().getProfilePhoto() != null) {
            photoPath = "." + s.getUser().getProfilePhoto();
            log.info("Photo path: {}", photoPath);
        }
        if (photoPath != null && Files.exists(Paths.get(photoPath))) {
            try {
                Image img = Image.getInstance(photoPath);
                img.scaleToFit(80, 90);
                photoCell.addElement(img);
            } catch (Exception e) {
                addPhotoPlaceholder(photoCell, s.getUser().getName());
            }
        } else {
            addPhotoPlaceholder(photoCell, s.getUser().getName());
        }
        outer.addCell(photoCell);

        // Student details cell
        PdfPCell details = new PdfPCell();
        details.setBorder(Rectangle.BOX);
        details.setBorderColor(BORDER);
        details.setBackgroundColor(LIGHT_BG);
        details.setPadding(10);

        Font sectionTitle = new Font(Font.HELVETICA, 8, Font.BOLD, INDIGO);
        Font lbl = new Font(Font.HELVETICA, 8,  Font.BOLD,   TEXT_MUTE);
        Font val = new Font(Font.HELVETICA, 9,  Font.BOLD,   TEXT_MAIN);

        details.addElement(new Phrase("STUDENT INFORMATION\n", sectionTitle));
        addDetailRow(details, "Name",           s.getUser().getName(), lbl, val);
        addDetailRow(details, "Roll Number",    s.getRollNumber(),      lbl, val);
        addDetailRow(details, "Student No.",    s.getStudentNo(),       lbl, val);
        addDetailRow(details, "Course/Branch",  s.getCourseBranch() + " (Year " + (s.getYear() != null ? s.getYear() : "—") + ")", lbl, val);
        addDetailRow(details, "Hostel",         s.getHostelName(),      lbl, val);
        addDetailRow(details, "Room No.",       s.getRoomNumber(),      lbl, val);
        addDetailRow(details, "Mobile",         s.getMobileNumber() != null ? s.getMobileNumber() : "—", lbl, val);
        outer.addCell(details);

        // QR Cell


        doc.add(outer);
    }

    private void addPhotoPlaceholder(PdfPCell cell, String name) {
        Font f = new Font(Font.HELVETICA, 22, Font.BOLD, NAVY);
        Paragraph p = new Paragraph(String.valueOf(name.charAt(0)).toUpperCase(), f);
        p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);
        cell.addElement(new Phrase("Photo", new Font(Font.HELVETICA, 7, Font.ITALIC, TEXT_MUTE)));
    }

    private void addDetailRow(PdfPCell cell, String label, String value, Font lbl, Font val) {
        cell.addElement(new Phrase(label + ": ", lbl));
        cell.addElement(new Phrase(value + "\n", val));
    }

    // ── LEAVE DETAILS ─────────────────────────────────────────────────────────
    private void addLeaveDetailsSection(Document doc, LeaveRequest leave) throws DocumentException {
        doc.add(sectionHeader("LEAVE DETAILS"));

        PdfPTable t = new PdfPTable(new float[]{1f, 1.5f, 1f, 1.5f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(8);

        Font lbl = new Font(Font.HELVETICA, 8, Font.BOLD,   TEXT_MUTE);
        Font val = new Font(Font.HELVETICA, 9, Font.NORMAL, TEXT_MAIN);

        addRow4(t, "Leave Type",    leave.getLeaveType().name().replace("_"," "),
                   "Total Days",    leave.getLeaveDays() + " day(s)", lbl, val);
        addRow4(t, "From Date",     leave.getFromDate().format(DATE_FMT),
                   "To Date",       leave.getToDate().format(DATE_FMT), lbl, val);
        addRow4(t, "Time Out",      leave.getTimeOut() != null ? leave.getTimeOut().toString() : "—",
                   "Attendance",    leave.getAttendancePercentage() != null ? leave.getAttendancePercentage() + "%" : "—", lbl, val);

        // Reason spans full row
        PdfPCell reasonLabel = cell(new Phrase("Reason for Leave:", lbl), LIGHT_BG);
        t.addCell(reasonLabel);
        PdfPCell reasonVal = cell(new Phrase(leave.getReason(), val), Color.WHITE);
        reasonVal.setColspan(3);
        t.addCell(reasonVal);

        if (leave.getVisitPersonName() != null && !leave.getVisitPersonName().isEmpty()) {
            addRow4(t, "Person Visited",   leave.getVisitPersonName(),
                       "Relation",          leave.getVisitPersonRelation() != null ? leave.getVisitPersonRelation() : "—", lbl, val);
            addRow4(t, "Contact",           leave.getVisitPersonContact() != null ? leave.getVisitPersonContact() : "—",
                       "Emergency Contact", leave.getEmergencyContact()   != null ? leave.getEmergencyContact()   : "—", lbl, val);
            if (leave.getVisitPersonAddress() != null) {
                PdfPCell addrLbl = cell(new Phrase("Address:", lbl), LIGHT_BG);
                t.addCell(addrLbl);
                PdfPCell addrVal = cell(new Phrase(leave.getVisitPersonAddress(), val), Color.WHITE);
                addrVal.setColspan(3);
                t.addCell(addrVal);
            }
        }

        if (leave.getHodName() != null) {
            addRow4(t, "HoD Name", leave.getHodName(), "HoD Verification", "Verified", lbl, val);
        }

        doc.add(t);
    }

    // ── APPROVAL CHAIN ────────────────────────────────────────────────────────
    private void addApprovalSection(Document doc, LeaveRequest leave) throws DocumentException {
        doc.add(sectionHeader("APPROVAL CHAIN"));

        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        t.setSpacingAfter(8);

        Font hdr = new Font(Font.HELVETICA, 9,  Font.BOLD, TEXT_MUTE);
        Font apv = new Font(Font.HELVETICA, 10, Font.BOLD, EMERALD);
        Font rmk = new Font(Font.HELVETICA, 8,  Font.ITALIC, TEXT_MUTE);
        Font tim = new Font(Font.HELVETICA, 7,  Font.NORMAL, TEXT_MUTE);

        // Headers
        for (String h : new String[]{"PARENT", "WARDEN", "DEAN / DIRECTOR"}) {
            PdfPCell hc = cell(new Phrase(h, hdr), LIGHT_BG);
            hc.setHorizontalAlignment(Element.ALIGN_CENTER);
            t.addCell(hc);
        }

        // Status row
        for (int i = 0; i < 3; i++) {
            PdfPCell sc = new PdfPCell();
            sc.setBorderColor(BORDER);
            sc.setPadding(10);
            sc.setHorizontalAlignment(Element.ALIGN_CENTER);
            String time = "";
            String remark = "";
            if (i == 0) {
                time   = leave.getParentApprovedAt()  != null ? leave.getParentApprovedAt().format(DT_FMT)  : "";
            } else if (i == 1) {
                time   = leave.getWardenApprovedAt()  != null ? leave.getWardenApprovedAt().format(DT_FMT)  : "";
                remark = leave.getWardenRemarks()      != null ? leave.getWardenRemarks()    : "";
            } else {
                time   = leave.getDeanApprovedAt()    != null ? leave.getDeanApprovedAt().format(DT_FMT)    : "";
                remark = leave.getDeanRemarks()        != null ? leave.getDeanRemarks()      : "";
            }
            sc.addElement(new Phrase("✓  APPROVED\n", apv));
            if (!time.isEmpty())   sc.addElement(new Phrase(time + "\n", tim));
            if (!remark.isEmpty()) sc.addElement(new Phrase("\"" + remark + "\"", rmk));
            t.addCell(sc);
        }
        doc.add(t);
    }

    // ── WARDEN REMARKS ────────────────────────────────────────────────────────
    private void addWardenRemarksSection(Document doc, LeaveRequest leave) throws DocumentException {
        doc.add(sectionHeader("WARDEN'S REMARKS"));
        PdfPTable t = new PdfPTable(new float[]{1.2f, 0.8f, 2f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(8);

        Font lbl = new Font(Font.HELVETICA, 8, Font.BOLD,   TEXT_MUTE);
        Font val = new Font(Font.HELVETICA, 9, Font.NORMAL, TEXT_MAIN);

        t.addCell(cell(new Phrase("No. of Working Days\nDuring Leave", lbl), LIGHT_BG));
        t.addCell(cell(new Phrase(leave.getWorkingDaysCount() != null ? String.valueOf(leave.getWorkingDaysCount()) : "—", val), Color.WHITE));
        t.addCell(cell(new Phrase("Communication with Parents / Time:\n" +
                (leave.getWardenParentCommTime() != null ? leave.getWardenParentCommTime() : "—"), val), Color.WHITE));
        doc.add(t);
    }

    // ── GATE PASS STATUS ──────────────────────────────────────────────────────
//    private void addGatePassStatus(Document doc, GatePass gatePass) throws DocumentException {
//        doc.add(sectionHeader("GATE PASS STATUS"));
//        PdfPTable t = new PdfPTable(2);
//        t.setWidthPercentage(100);
//        t.setSpacingAfter(8);
//
//        Font lbl = new Font(Font.HELVETICA, 8, Font.BOLD,   TEXT_MUTE);
//        Font val = new Font(Font.HELVETICA, 9, Font.NORMAL, TEXT_MAIN);
//
//        t.addCell(cell(new Phrase("Exit Time", lbl), LIGHT_BG));
//        t.addCell(cell(new Phrase(gatePass.getExitTime()  != null ? gatePass.getExitTime().format(DT_FMT)  : "Not yet exited",  val), Color.WHITE));
//        t.addCell(cell(new Phrase("Entry Time", lbl), LIGHT_BG));
//        t.addCell(cell(new Phrase(gatePass.getEntryTime() != null ? gatePass.getEntryTime().format(DT_FMT) : "Not yet returned", val), Color.WHITE));
//        doc.add(t);
//    }

    // ── SIGNATURES ────────────────────────────────────────────────────────────
//    private void addSignatureSection(Document doc) throws DocumentException {
//        PdfPTable t = new PdfPTable(3);
//        t.setWidthPercentage(100);
//        t.setSpacingAfter(10);
//        t.setSpacingBefore(8);
//
//        Font f = new Font(Font.HELVETICA, 8, Font.BOLD, TEXT_MUTE);
//        for (String s : new String[]{"Student's Signature", "Warden's Signature", "Dean / Director's Signature"}) {
//            PdfPCell c = new PdfPCell();
//            c.setBorderColor(BORDER);
//            c.setPadding(14);
//            c.setHorizontalAlignment(Element.ALIGN_CENTER);
//            c.addElement(new Phrase("\n\n\n____________________________\n" + s, f));
//            t.addCell(c);
//        }
//        doc.add(t);
//    }

    // ── FOOTER NOTES ──────────────────────────────────────────────────────────
    private void addFooter(Document doc, LeaveRequest leave) throws DocumentException {
        Font note = new Font(Font.HELVETICA, 7, Font.ITALIC, TEXT_MUTE);
        Font gen  = new Font(Font.HELVETICA, 6, Font.NORMAL, new Color(203, 213, 225));

        String[] notes = {
            "* Leave from hostel does not mean leave from college/classes.",
            "* Leave on holidays may be granted by the Warden.",
            "* Leave on working days up to 3 days: Associate Dean (Girls) / Dean (Boys).",
            "* Leave beyond 3 working days: Director General on Dean's recommendation."
        };
        for (String n : notes) doc.add(new Phrase(n + "\n", note));
        doc.add(Chunk.NEWLINE);
        doc.add(new Phrase("Generated by AKGEC Smart Hostel Leave Management Portal  •  " +
                java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")), gen));
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────
    private PdfPTable sectionHeader(String title) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(6);

        Font f = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        PdfPCell c = new PdfPCell(new Phrase("  " + title, f));
        c.setBackgroundColor(INDIGO);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(6);
        t.addCell(c);
        return t;
    }

    private PdfPCell cell(Phrase content, Color bg) {
        PdfPCell c = new PdfPCell(content);
        c.setBorderColor(BORDER);
        c.setBackgroundColor(bg);
        c.setPadding(7);
        return c;
    }

    private void addRow4(PdfPTable t, String l1, String v1, String l2, String v2, Font lbl, Font val) {
        t.addCell(cell(new Phrase(l1 + ":", lbl), LIGHT_BG));
        t.addCell(cell(new Phrase(v1,         val), Color.WHITE));
        t.addCell(cell(new Phrase(l2 + ":", lbl), LIGHT_BG));
        t.addCell(cell(new Phrase(v2,         val), Color.WHITE));
    }
}
