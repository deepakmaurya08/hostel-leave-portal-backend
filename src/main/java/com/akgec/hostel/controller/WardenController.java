package com.akgec.hostel.controller;

import com.akgec.hostel.dto.request.ApprovalRequest;
import com.akgec.hostel.dto.response.*;
import com.akgec.hostel.model.enums.LeaveStatus;
import com.akgec.hostel.repository.LeaveRequestRepository;
import com.akgec.hostel.service.impl.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/warden")
@RequiredArgsConstructor
@PreAuthorize("hasRole('WARDEN')")
public class WardenController {

    private final LeaveService leaveService;
    private final LeaveRequestRepository leaveRequestRepository;

    /**
     * GET /api/warden/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<WardenDashboardResponse>> getDashboard() {
        List<LeaveResponse> pending = leaveService.getPendingWardenLeaves();

        long approvedToday = leaveRequestRepository.findLeavesSince(
                LocalDateTime.now().withHour(0).withMinute(0))
                .stream().filter(l -> l.getStatus() == LeaveStatus.PENDING_DEAN).count();

        long rejectedToday = leaveRequestRepository.findLeavesSince(
                LocalDateTime.now().withHour(0).withMinute(0))
                .stream().filter(l -> l.getStatus() == LeaveStatus.WARDEN_REJECTED).count();

        WardenDashboardResponse dashboard = WardenDashboardResponse.builder()
                .pendingRequests(pending.size())
                .approvedToday(approvedToday)
                .rejectedToday(rejectedToday)
                .escalatedToDean(leaveRequestRepository.countByStatus(LeaveStatus.PENDING_DEAN))
                .pendingLeaves(pending)
                .build();

        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    /**
     * GET /api/warden/leaves/pending
     */
    @GetMapping("/leaves/pending")
    public ResponseEntity<ApiResponse<List<LeaveResponse>>> getPendingLeaves() {
        return ResponseEntity.ok(ApiResponse.success(leaveService.getPendingWardenLeaves()));
    }

    /**
     * GET /api/warden/leaves/{leaveId}
     */
    @GetMapping("/leaves/{leaveId}")
    public ResponseEntity<ApiResponse<LeaveResponse>> getLeave(@PathVariable Long leaveId) {
        return ResponseEntity.ok(ApiResponse.success(leaveService.getLeaveById(leaveId)));
    }

    /**
     * POST /api/warden/leaves/{leaveId}/approve
     */
    @PostMapping("/leaves/{leaveId}/approve")
    public ResponseEntity<ApiResponse<LeaveResponse>> approveLeave(
            @PathVariable Long leaveId,
            @RequestBody(required = false) ApprovalRequest request) {
        String remarks = request != null ? request.getRemarks() : null;
        Integer workingDays = request != null ? request.getWorkingDaysCount() : null;
        String commTime = request != null ? request.getWardenParentCommTime() : null;
        LeaveResponse response = leaveService.wardenApprove(leaveId, remarks, workingDays, commTime);
        return ResponseEntity.ok(ApiResponse.success(response, "Leave approved and forwarded to Dean"));
    }

    /**
     * POST /api/warden/leaves/{leaveId}/reject
     */
    @PostMapping("/leaves/{leaveId}/reject")
    public ResponseEntity<ApiResponse<LeaveResponse>> rejectLeave(
            @PathVariable Long leaveId,
            @RequestBody(required = false) ApprovalRequest request) {
        String remarks = request != null ? request.getRemarks() : null;
        LeaveResponse response = leaveService.wardenReject(leaveId, remarks);
        return ResponseEntity.ok(ApiResponse.success(response, "Leave rejected"));
    }

    /**
     * GET /api/warden/leaves/history
     * All leaves (approved + rejected) for audit view
     */
    @GetMapping("/leaves/history")
    public ResponseEntity<ApiResponse<List<LeaveResponse>>> getHistory() {
        List<LeaveResponse> history = leaveRequestRepository
                .findLeavesSince(LocalDateTime.now().minusDays(30))
                .stream()
                .map(l -> leaveService.buildLeaveResponse(l, false))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
