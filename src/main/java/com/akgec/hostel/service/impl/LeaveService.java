package com.akgec.hostel.service.impl;

import com.akgec.hostel.dto.request.LeaveApplicationRequest;
import com.akgec.hostel.dto.response.ApprovalHistoryResponse;
import com.akgec.hostel.dto.response.LeaveResponse;
import com.akgec.hostel.dto.response.StudentDashboardResponse;
import com.akgec.hostel.exception.HostelException;
import com.akgec.hostel.model.entity.*;
import com.akgec.hostel.model.enums.ApprovalAction;
import com.akgec.hostel.model.enums.LeaveStatus;
import com.akgec.hostel.model.enums.Role;
import com.akgec.hostel.notification.EmailNotificationService;
import com.akgec.hostel.repository.*;
import com.akgec.hostel.util.LeavePassNumberGenerator;
import com.akgec.hostel.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final StudentRepository studentRepository;
    private final ParentTokenRepository parentTokenRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;
    private final GatePassRepository gatePassRepository;
    private final UserRepository userRepository;
    private final EmailNotificationService emailService;
    private final LeavePassNumberGenerator passNumberGenerator;
    private final SecurityUtils securityUtils;
    private final DocumentService documentService;


    // ── Apply Leave ───────────────────────────────────────────────────────────
    @Transactional
    public LeaveResponse applyLeave(LeaveApplicationRequest request, List<MultipartFile> documents) {
        Long userId = securityUtils.getCurrentUserId();
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new HostelException.ResourceNotFoundException("Student profile not found"));

        if (!request.getToDate().isAfter(request.getFromDate()))
            throw new HostelException.InvalidRequestException("To date must be after from date");

        List<LeaveRequest> overlapping = leaveRequestRepository.findOverlappingLeaves(
                student.getId(), request.getFromDate(), request.getToDate());
        if (!overlapping.isEmpty())
            throw new HostelException.LeaveWorkflowException("You already have a leave for this period");

        LeaveRequest leave = LeaveRequest.builder()
                .leavePassNumber(passNumberGenerator.generate())
                .student(student)
                .leaveType(request.getLeaveType())
                .reason(request.getReason())
                .fromDate(request.getFromDate())
                .toDate(request.getToDate())
                .timeOut(request.getTimeOut())
                .visitPersonName(request.getVisitPersonName())
                .visitPersonRelation(request.getVisitPersonRelation())
                .visitPersonAddress(request.getVisitPersonAddress())
                .visitPersonContact(request.getVisitPersonContact())
                .emergencyContact(request.getEmergencyContact() != null ? request.getEmergencyContact() : student.getEmergencyContact())
                .attendancePercentage(request.getAttendancePercentage())
                .hodName(request.getHodName())
                .status(LeaveStatus.PENDING_PARENT)
                .build();
        leave = leaveRequestRepository.save(leave);

        documentService.saveDocuments(leave, documents);

        recordHistory(leave, "STUDENT:" + student.getUser().getEmail(), Role.ROLE_STUDENT,
                ApprovalAction.SUBMITTED, "Leave application submitted");

        // Email ONLY to parent (not warden/dean)
        String approveToken = generateParentToken(leave);
        String rejectToken  = generateParentToken(leave);
        emailService.sendParentApprovalEmail(leave, approveToken, rejectToken);
        emailService.sendLeaveSubmittedEmail(leave);

        log.info("Leave applied: {} by {}", leave.getLeavePassNumber(), student.getRollNumber());
        return buildLeaveResponse(leave, false);
    }

    // ── Student dashboard ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public StudentDashboardResponse getStudentDashboard() {
        Long userId = securityUtils.getCurrentUserId();
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new HostelException.ResourceNotFoundException("Student profile not found"));

        List<LeaveRequest> all = leaveRequestRepository.findByStudentIdOrderByCreatedAtDesc(student.getId());

        long pending  = all.stream().filter(l -> l.getStatus().name().startsWith("PENDING")).count();
        long approved = all.stream().filter(l -> l.getStatus() == LeaveStatus.APPROVED || l.getStatus() == LeaveStatus.COMPLETED).count();
        long rejected = all.stream().filter(l -> l.getStatus().name().endsWith("REJECTED")).count();

        return StudentDashboardResponse.builder()
                .studentName(student.getUser().getName())
                .rollNumber(student.getRollNumber())
                .hostelName(student.getHostelName())
                .roomNumber(student.getRoomNumber())
                .totalLeavesTaken(all.size())
                .pendingRequests(pending)
                .approvedLeaves(approved)
                .rejectedLeaves(rejected)
                .recentLeaves(all.stream().limit(5).map(l -> buildLeaveResponse(l, false)).collect(Collectors.toList()))
                .build();
    }

    @Transactional(readOnly = true)
    public List<LeaveResponse> getMyLeaves() {
        Long userId = securityUtils.getCurrentUserId();
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new HostelException.ResourceNotFoundException("Student profile not found"));
        return leaveRequestRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())
                .stream().map(l -> buildLeaveResponse(l, false)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LeaveResponse getLeaveById(Long leaveId) {
        return buildLeaveResponse(findLeaveById(leaveId), true);
    }

    // ── Warden approve/reject (NO email to warden — dashboard only) ───────────
    @Transactional(readOnly = true)
    public List<LeaveResponse> getPendingWardenLeaves() {
        return leaveRequestRepository.findPendingWardenRequests()
                .stream().map(l -> buildLeaveResponse(l, true)).collect(Collectors.toList());
    }

    @Transactional
    public LeaveResponse wardenApprove(Long leaveId, String remarks, Integer workingDays, String parentCommTime) {
        LeaveRequest leave = findLeaveById(leaveId);
        validateStatus(leave, LeaveStatus.PENDING_WARDEN, "Warden");

        User warden = userRepository.findById(securityUtils.getCurrentUserId())
                .orElseThrow(() -> new HostelException.ResourceNotFoundException("Warden not found"));

        leave.setStatus(LeaveStatus.PENDING_DEAN);
        leave.setWardenRemarks(remarks);
        leave.setWorkingDaysCount(workingDays);
        leave.setWardenParentCommTime(parentCommTime);
        leave.setApprovedByWarden(warden);
        leave.setWardenApprovedAt(LocalDateTime.now());
        leave = leaveRequestRepository.save(leave);

        recordHistory(leave, warden.getEmail(), Role.ROLE_WARDEN, ApprovalAction.WARDEN_APPROVED, remarks);
        // NO email to dean — request appears on dean's dashboard only
        log.info("Leave {} approved by warden {}", leave.getLeavePassNumber(), warden.getEmail());
        return buildLeaveResponse(leave, true);
    }

    @Transactional
    public LeaveResponse wardenReject(Long leaveId, String remarks) {
        LeaveRequest leave = findLeaveById(leaveId);
        validateStatus(leave, LeaveStatus.PENDING_WARDEN, "Warden");

        User warden = userRepository.findById(securityUtils.getCurrentUserId())
                .orElseThrow(() -> new HostelException.ResourceNotFoundException("Warden not found"));

        leave.setStatus(LeaveStatus.WARDEN_REJECTED);
        leave.setWardenRemarks(remarks);
        leave.setApprovedByWarden(warden);
        leave = leaveRequestRepository.save(leave);

        recordHistory(leave, warden.getEmail(), Role.ROLE_WARDEN, ApprovalAction.WARDEN_REJECTED, remarks);
        // Email student that warden rejected
        emailService.sendWardenRejectedEmail(leave);
        return buildLeaveResponse(leave, true);
    }

    // ── Dean approve/reject (NO email to dean — dashboard only) ──────────────
    @Transactional(readOnly = true)
    public List<LeaveResponse> getPendingDeanLeaves() {
        return leaveRequestRepository.findPendingDeanRequests()
                .stream().map(l -> buildLeaveResponse(l, true)).collect(Collectors.toList());
    }

    @Transactional
    public LeaveResponse deanApprove(Long leaveId, String remarks, GatePassService gatePassService) {
        LeaveRequest leave = findLeaveById(leaveId);
        validateStatus(leave, LeaveStatus.PENDING_DEAN, "Dean");

        User dean = userRepository.findById(securityUtils.getCurrentUserId())
                .orElseThrow(() -> new HostelException.ResourceNotFoundException("Dean not found"));

        leave.setStatus(LeaveStatus.APPROVED);
        leave.setDeanRemarks(remarks);
        leave.setApprovedByDean(dean);
        leave.setDeanApprovedAt(LocalDateTime.now());
        leave = leaveRequestRepository.save(leave);

        recordHistory(leave, dean.getEmail(), Role.ROLE_DEAN, ApprovalAction.DEAN_APPROVED, remarks);
        gatePassService.generateGatePass(leave);
        // Email student that leave is fully approved
        emailService.sendLeaveApprovedEmail(leave);
        return buildLeaveResponse(leave, true);
    }

    @Transactional
    public LeaveResponse deanReject(Long leaveId, String remarks) {
        LeaveRequest leave = findLeaveById(leaveId);
        validateStatus(leave, LeaveStatus.PENDING_DEAN, "Dean");

        User dean = userRepository.findById(securityUtils.getCurrentUserId())
                .orElseThrow(() -> new HostelException.ResourceNotFoundException("Dean not found"));

        leave.setStatus(LeaveStatus.DEAN_REJECTED);
        leave.setDeanRemarks(remarks);
        leave.setApprovedByDean(dean);
        leave = leaveRequestRepository.save(leave);

        recordHistory(leave, dean.getEmail(), Role.ROLE_DEAN, ApprovalAction.DEAN_REJECTED, remarks);
        emailService.sendDeanRejectedEmail(leave);
        return buildLeaveResponse(leave, true);
    }

    // ── Cancel ────────────────────────────────────────────────────────────────
    @Transactional
    public LeaveResponse cancelLeave(Long leaveId) {
        Long userId = securityUtils.getCurrentUserId();
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new HostelException.ResourceNotFoundException("Student not found"));
        LeaveRequest leave = findLeaveById(leaveId);
        if (!leave.getStudent().getId().equals(student.getId()))
            throw new HostelException.AccessDeniedException("You can only cancel your own leave");
        if (leave.getStatus() == LeaveStatus.APPROVED || leave.getStatus() == LeaveStatus.COMPLETED)
            throw new HostelException.LeaveWorkflowException("Cannot cancel approved/completed leave");

        leave.setStatus(LeaveStatus.CANCELLED);
        leave = leaveRequestRepository.save(leave);
        recordHistory(leave, student.getUser().getEmail(), Role.ROLE_STUDENT, ApprovalAction.CANCELLED, "Cancelled by student");
        return buildLeaveResponse(leave, false);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    public LeaveRequest findLeaveById(Long leaveId) {
        return leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new HostelException.ResourceNotFoundException("Leave", "id", leaveId));
    }

    private void validateStatus(LeaveRequest leave, LeaveStatus expected, String role) {
        if (leave.getStatus() != expected)
            throw new HostelException.LeaveWorkflowException(
                    role + " can only act on leaves with status: " + expected.name() + ". Current: " + leave.getStatus());
    }

    private String generateParentToken(LeaveRequest leave) {
        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        parentTokenRepository.save(ParentToken.builder()
                .leaveRequest(leave).token(token)
                .expiryTime(LocalDateTime.now().plusHours(48)).used(false).build());
        return token;
    }

    public void recordHistory(LeaveRequest leave, String performedBy, Role role, ApprovalAction action, String remarks) {
        approvalHistoryRepository.save(ApprovalHistory.builder()
                .leaveRequest(leave).performedBy(performedBy).role(role).action(action).remarks(remarks).build());
    }

    public LeaveResponse buildLeaveResponse(LeaveRequest leave, boolean withTimeline) {
        Student s = leave.getStudent();
        LeaveResponse.LeaveResponseBuilder b = LeaveResponse.builder()
                .id(leave.getId()).leavePassNumber(leave.getLeavePassNumber())
                .studentName(s.getUser().getName()).rollNumber(s.getRollNumber())
                .studentNo(s.getStudentNo()).hostelName(s.getHostelName())
                .roomNumber(s.getRoomNumber()).courseBranch(s.getCourseBranch())
                .leaveType(leave.getLeaveType()).reason(leave.getReason())
                .fromDate(leave.getFromDate()).toDate(leave.getToDate())
                .timeOut(leave.getTimeOut()).leaveDays(leave.getLeaveDays())
                .visitPersonName(leave.getVisitPersonName()).visitPersonRelation(leave.getVisitPersonRelation())
                .visitPersonAddress(leave.getVisitPersonAddress()).visitPersonContact(leave.getVisitPersonContact())
                .attendancePercentage(leave.getAttendancePercentage()).hodName(leave.getHodName())
                .status(leave.getStatus())
                .wardenRemarks(leave.getWardenRemarks()).deanRemarks(leave.getDeanRemarks())
                .workingDaysCount(leave.getWorkingDaysCount()).wardenParentCommTime(leave.getWardenParentCommTime())
                .approvedByWarden(leave.getApprovedByWarden() != null ? leave.getApprovedByWarden().getName() : null)
                .approvedByDean(leave.getApprovedByDean() != null ? leave.getApprovedByDean().getName() : null)
                .parentApprovedAt(leave.getParentApprovedAt()).wardenApprovedAt(leave.getWardenApprovedAt())
                .deanApprovedAt(leave.getDeanApprovedAt()).createdAt(leave.getCreatedAt());

        if (leave.getDocuments() != null && !leave.getDocuments().isEmpty()) {
            List<String> urls = leave.getDocuments()
                    .stream()
                    .map(doc -> "/documents/" + doc.getId())
                    .collect(Collectors.toList());

            b.documentUrls(urls);
        }
        // Build document URLs

        if (leave.getGatePass() != null) {
            GatePass gp = leave.getGatePass();
            b.gatePass(LeaveResponse.GatePassInfo.builder()
                    .qrToken(gp.getQrToken()).exitTime(gp.getExitTime())
                    .entryTime(gp.getEntryTime()).pdfReady(gp.getPdfPath() != null).build());
        }


        if (withTimeline) {
            b.timeline(approvalHistoryRepository.findByLeaveRequestIdOrderByTimestampAsc(leave.getId())
                    .stream().map(h -> ApprovalHistoryResponse.builder()
                            .id(h.getId()).performedBy(h.getPerformedBy()).role(h.getRole())
                            .action(h.getAction()).remarks(h.getRemarks()).timestamp(h.getTimestamp()).build())
                    .collect(Collectors.toList()));
        }
        return b.build();
    }
}
