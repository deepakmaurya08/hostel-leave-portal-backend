package com.akgec.hostel.service.impl;

import com.akgec.hostel.dto.response.QrScanResponse;
import com.akgec.hostel.exception.HostelException;
import com.akgec.hostel.model.entity.GatePass;
import com.akgec.hostel.model.entity.LeaveRequest;
import com.akgec.hostel.model.entity.Student;
import com.akgec.hostel.model.entity.User;
import com.akgec.hostel.model.enums.ApprovalAction;
import com.akgec.hostel.model.enums.LeaveStatus;
import com.akgec.hostel.model.enums.Role;
import com.akgec.hostel.pdf.LeavePdfGenerator;
import com.akgec.hostel.qr.QrCodeGenerator;
import com.akgec.hostel.repository.GatePassRepository;
import com.akgec.hostel.repository.LeaveRequestRepository;
import com.akgec.hostel.repository.UserRepository;
import com.akgec.hostel.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GatePassService {

    private final GatePassRepository gatePassRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final QrCodeGenerator qrCodeGenerator;
    private final LeavePdfGenerator leavePdfGenerator;
    private final LeaveService leaveService;
    private final SecurityUtils securityUtils;

    @Value("${app.file.pdf-dir}")
    private String pdfDir;

    // ─────────────────────────────────────────────────────────────────────────
    // Called by LeaveService after Dean approval
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public GatePass generateGatePass(LeaveRequest leave) {
        try {
            Student student = leave.getStudent();

            // Generate QR token
            String qrToken = qrCodeGenerator.generateQrToken(leave.getId(), student.getId());

            // Save gate pass first (PDF generation needs it)
            GatePass gatePass = GatePass.builder()
                    .leaveRequest(leave)
                    .qrToken(qrToken)
                    .build();
            gatePass = gatePassRepository.save(gatePass);

            // Generate QR image
            String qrImagePath = qrCodeGenerator.generateQrCodeImage(qrToken, leave.getLeavePassNumber());
            gatePass.setQrImagePath(qrImagePath);

            // Generate PDF
            String pdfPath = leavePdfGenerator.generateLeavePdf(leave, gatePass);
            gatePass.setPdfPath(pdfPath);

            gatePass = gatePassRepository.save(gatePass);

            // Record history
            leaveService.recordHistory(leave, "SYSTEM", Role.ROLE_ADMIN,
                    ApprovalAction.PASS_GENERATED, "Gate pass and QR code generated");

            log.info("Gate pass generated for leave: {}", leave.getLeavePassNumber());
            return gatePass;

        } catch (Exception e) {
            log.error("Failed to generate gate pass for leave {}: {}", leave.getLeavePassNumber(), e.getMessage(), e);
            throw new RuntimeException("Gate pass generation failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // QR Scan - Verify and return student/leave info
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public QrScanResponse scanQr(String tokenOrPassNumber) {
        GatePass gatePass = gatePassRepository.findByQrToken(tokenOrPassNumber)
                .orElseGet(() -> gatePassRepository.findByLeaveRequestLeavePassNumber(tokenOrPassNumber)
                        .orElseThrow(() -> new HostelException.ResourceNotFoundException("Invalid QR Code")));

        LeaveRequest leave = gatePass.getLeaveRequest();
        Student student = leave.getStudent();

        // Validity checks
        boolean isApproved = leave.getStatus() == LeaveStatus.APPROVED
                || leave.getStatus() == LeaveStatus.COMPLETED;
        boolean isWithinPeriod = !LocalDate.now().isBefore(leave.getFromDate())
                && !LocalDate.now().isAfter(leave.getToDate().plusDays(1)); // +1 grace for return
        boolean alreadyCompleted = leave.getStatus() == LeaveStatus.COMPLETED;

        if (!isApproved) {
            return QrScanResponse.builder()
                    .valid(false)
                    .message("Leave is not approved. Status: " + leave.getStatus())
                    .nextAction("INVALID")
                    .build();
        }

        // Determine next action
        String nextAction;
        String message;
        if (!gatePass.hasExited()) {
            if (!isWithinPeriod) {
                nextAction = "INVALID";
                message = "Student is trying to exit outside the approved leave period";
            } else {
                nextAction = "MARK_EXIT";
                message = "Student verified. Ready to mark EXIT.";
            }
        } else if (!gatePass.hasReturned()) {
            nextAction = "MARK_ENTRY";
            message = "Student returning. Ready to mark ENTRY.";
        } else {
            nextAction = "COMPLETED";
            message = "Leave already completed. Student has returned.";
        }

        return QrScanResponse.builder()
                .valid(true)
                .message(message)
                .studentName(student.getUser().getName())
                .rollNumber(student.getRollNumber())
                .studentNo(student.getStudentNo())
                .hostelName(student.getHostelName())
                .roomNumber(student.getRoomNumber())
                .leavePassNumber(leave.getLeavePassNumber())
                .fromDate(leave.getFromDate())
                .toDate(leave.getToDate())
                .leaveType(leave.getLeaveType().name())
                .reason(leave.getReason())
                .exitDone(gatePass.hasExited())
                .entryDone(gatePass.hasReturned())
                .exitTime(gatePass.getExitTime())
                .entryTime(gatePass.getEntryTime())
                .parentApproved(leave.getParentApprovedAt() != null)
                .wardenApproved(leave.getWardenApprovedAt() != null)
                .deanApproved(leave.getDeanApprovedAt() != null)
                .nextAction(nextAction)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mark Exit
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public QrScanResponse markExit(String qrToken, String remarks) {
        GatePass gatePass = gatePassRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new HostelException.ResourceNotFoundException("Invalid QR Code"));

        LeaveRequest leave = gatePass.getLeaveRequest();

        if (leave.getStatus() != LeaveStatus.APPROVED) {
            throw new HostelException.LeaveWorkflowException("Leave is not in APPROVED status");
        }
        if (gatePass.hasExited()) {
            throw new HostelException.LeaveWorkflowException("Exit already marked at: " + gatePass.getExitTime());
        }

        Long guardUserId = securityUtils.getCurrentUserId();
        User guard = userRepository.findById(guardUserId)
                .orElseThrow(() -> new HostelException.ResourceNotFoundException("Security guard not found"));

        gatePass.setExitTime(LocalDateTime.now());
        gatePass.setExitMarkedBy(guard);
        gatePass.setExitRemarks(remarks);
        gatePassRepository.save(gatePass);

        leaveService.recordHistory(leave, guard.getEmail(), Role.ROLE_SECURITY,
                ApprovalAction.EXIT_MARKED, "Exit marked by guard: " + guard.getName()
                        + (remarks != null ? " | " + remarks : ""));

        log.info("EXIT marked for leave {} by guard {}", leave.getLeavePassNumber(), guard.getEmail());
        return scanQr(qrToken);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mark Entry
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public QrScanResponse markEntry(String qrToken, String remarks) {
        GatePass gatePass = gatePassRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new HostelException.ResourceNotFoundException("Invalid QR Code"));

        LeaveRequest leave = gatePass.getLeaveRequest();

        if (!gatePass.hasExited()) {
            throw new HostelException.LeaveWorkflowException("Cannot mark entry without exit being marked first");
        }
        if (gatePass.hasReturned()) {
            throw new HostelException.LeaveWorkflowException("Entry already marked at: " + gatePass.getEntryTime());
        }

        Long guardUserId = securityUtils.getCurrentUserId();
        User guard = userRepository.findById(guardUserId)
                .orElseThrow(() -> new HostelException.ResourceNotFoundException("Security guard not found"));

        gatePass.setEntryTime(LocalDateTime.now());
        gatePass.setEntryMarkedBy(guard);
        gatePass.setEntryRemarks(remarks);
        gatePassRepository.save(gatePass);

        // Complete the leave
        leave.setStatus(LeaveStatus.COMPLETED);
        leaveRequestRepository.save(leave);

        leaveService.recordHistory(leave, guard.getEmail(), Role.ROLE_SECURITY,
                ApprovalAction.ENTRY_MARKED, "Entry marked by guard: " + guard.getName()
                        + (remarks != null ? " | " + remarks : ""));

        log.info("ENTRY marked for leave {} by guard {}", leave.getLeavePassNumber(), guard.getEmail());
        return scanQr(qrToken);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Download PDF
    // ─────────────────────────────────────────────────────────────────────────
    public Resource downloadPdf(Long leaveId) {
        try {
            GatePass gatePass = gatePassRepository.findByLeaveRequestId(leaveId)
                    .orElseThrow(() -> new HostelException.ResourceNotFoundException("Gate pass not found for this leave"));

            if (gatePass.getPdfPath() == null) {
                throw new HostelException.ResourceNotFoundException("PDF not yet generated");
            }

            Path filePath = Paths.get(gatePass.getPdfPath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                throw new HostelException.ResourceNotFoundException("PDF file not found on server");
            }
            return resource;
        } catch (Exception e) {
            throw new HostelException.ResourceNotFoundException("Could not read PDF: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Security Guard Dashboard data
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<GatePass> getTodayExits() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        return gatePassRepository.findTodayExits(start, end);
    }

    @Transactional(readOnly = true)
    public List<GatePass> getTodayEntries() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        return gatePassRepository.findTodayEntries(start, end);
    }

    @Transactional(readOnly = true)
    public List<GatePass> getStudentsCurrentlyOnLeave() {
        return gatePassRepository.findStudentsCurrentlyOnLeave();
    }
}
