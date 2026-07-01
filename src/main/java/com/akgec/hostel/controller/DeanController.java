package com.akgec.hostel.controller;

import com.akgec.hostel.dto.request.ApprovalRequest;
import com.akgec.hostel.dto.response.ApiResponse;
import com.akgec.hostel.dto.response.LeaveResponse;
import com.akgec.hostel.model.enums.LeaveStatus;
import com.akgec.hostel.repository.LeaveRequestRepository;
import com.akgec.hostel.service.impl.GatePassService;
import com.akgec.hostel.service.impl.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dean")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DEAN')")
public class DeanController {

    private final LeaveService leaveService;
    private final GatePassService gatePassService;
    private final LeaveRequestRepository leaveRequestRepository;

    /**
     * GET /api/dean/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        List<LeaveResponse> pendingList = leaveService.getPendingDeanLeaves();

        // Priority: leaves with more days left before departure
        List<LeaveResponse> priorityRequests = pendingList.stream()
                .filter(l -> l.getLeaveDays() > 3)
                .collect(Collectors.toList());

        Map<String, Object> dashboard = Map.of(
                "pendingForApproval", pendingList.size(),
                "priorityRequests", priorityRequests.size(),
                "totalApproved", leaveRequestRepository.countByStatus(LeaveStatus.APPROVED),
                "totalCompleted", leaveRequestRepository.countByStatus(LeaveStatus.COMPLETED),
                "pendingLeaves", pendingList
        );
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    /**
     * GET /api/dean/leaves/pending
     */
    @GetMapping("/leaves/pending")
    public ResponseEntity<ApiResponse<List<LeaveResponse>>> getPendingLeaves() {
        return ResponseEntity.ok(ApiResponse.success(leaveService.getPendingDeanLeaves()));
    }

    /**
     * GET /api/dean/leaves/{leaveId}
     */
    @GetMapping("/leaves/{leaveId}")
    public ResponseEntity<ApiResponse<LeaveResponse>> getLeave(@PathVariable Long leaveId) {
        return ResponseEntity.ok(ApiResponse.success(leaveService.getLeaveById(leaveId)));
    }

    /**
     * POST /api/dean/leaves/{leaveId}/approve
     * Final approval - triggers QR + PDF generation
     */
    @PostMapping("/leaves/{leaveId}/approve")
    public ResponseEntity<ApiResponse<LeaveResponse>> approveLeave(
            @PathVariable Long leaveId,
            @RequestBody(required = false) ApprovalRequest request) {
        String remarks = request != null ? request.getRemarks() : null;
        LeaveResponse response = leaveService.deanApprove(leaveId, remarks, gatePassService);
        return ResponseEntity.ok(ApiResponse.success(response,
                "Leave approved. QR Code and PDF generated. Student has been notified."));
    }

    /**
     * POST /api/dean/leaves/{leaveId}/reject
     */
    @PostMapping("/leaves/{leaveId}/reject")
    public ResponseEntity<ApiResponse<LeaveResponse>> rejectLeave(
            @PathVariable Long leaveId,
            @RequestBody(required = false) ApprovalRequest request) {
        String remarks = request != null ? request.getRemarks() : null;
        LeaveResponse response = leaveService.deanReject(leaveId, remarks);
        return ResponseEntity.ok(ApiResponse.success(response, "Leave rejected"));
    }

    /**
     * GET /api/dean/leaves/approved
     * All fully approved leaves
     */
    @GetMapping("/leaves/approved")
    public ResponseEntity<ApiResponse<List<LeaveResponse>>> getApprovedLeaves() {
        List<LeaveResponse> approved = leaveRequestRepository
                .findByStatusOrderByCreatedAtAsc(LeaveStatus.APPROVED)
                .stream()
                .map(l -> leaveService.buildLeaveResponse(l, false))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(approved));
    }

    /**
     * GET /api/dean/leaves/history
     */
    @GetMapping("/leaves/history")
    public ResponseEntity<ApiResponse<List<LeaveResponse>>> getHistory(
            @RequestParam(defaultValue = "30") int days) {
        List<LeaveResponse> history = leaveRequestRepository
                .findLeavesSince(java.time.LocalDateTime.now().minusDays(days))
                .stream()
                .map(l -> leaveService.buildLeaveResponse(l, false))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
