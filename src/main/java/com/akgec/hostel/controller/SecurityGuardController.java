package com.akgec.hostel.controller;

import com.akgec.hostel.dto.response.ApiResponse;
import com.akgec.hostel.dto.response.LeaveResponse;
import com.akgec.hostel.dto.response.QrScanResponse;
import com.akgec.hostel.model.entity.GatePass;
import com.akgec.hostel.service.impl.GatePassService;
import com.akgec.hostel.service.impl.LeaveService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/security")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SECURITY')")
public class SecurityGuardController {

    private final GatePassService gatePassService;
    private final LeaveService leaveService;

    /**
     * GET /api/security/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        Map<String, Object> dashboard = Map.of(
                "studentsCurrentlyOnLeave", gatePassService.getStudentsCurrentlyOnLeave().size(),
                "todayExits", gatePassService.getTodayExits().size(),
                "todayEntries", gatePassService.getTodayEntries().size()
        );
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    /**
     * GET /api/security/scan?qrToken=xxx
     * Scan QR and get student/leave verification info
     */
    @GetMapping("/scan")
    public ResponseEntity<ApiResponse<QrScanResponse>> scanQr(@RequestParam String qrToken) {
        QrScanResponse response = gatePassService.scanQr(qrToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/security/mark-exit
     * Mark student exit after QR verification
     */
    @PostMapping("/mark-exit")
    public ResponseEntity<ApiResponse<QrScanResponse>> markExit(@RequestBody GateAction action) {
        QrScanResponse response = gatePassService.markExit(action.getQrToken(), action.getRemarks());
        return ResponseEntity.ok(ApiResponse.success(response, "EXIT marked successfully at " + java.time.LocalDateTime.now()));
    }

    /**
     * POST /api/security/mark-entry
     * Mark student return/entry after QR verification
     */
    @PostMapping("/mark-entry")
    public ResponseEntity<ApiResponse<QrScanResponse>> markEntry(@RequestBody GateAction action) {
        QrScanResponse response = gatePassService.markEntry(action.getQrToken(), action.getRemarks());
        return ResponseEntity.ok(ApiResponse.success(response, "ENTRY marked successfully. Leave completed."));
    }

    /**
     * GET /api/security/exits/today
     * Today's exits list
     */
    @GetMapping("/exits/today")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTodayExits() {
        List<Map<String, Object>> exits = gatePassService.getTodayExits().stream()
                .map(this::mapGatePassToSummary)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(exits));
    }

    /**
     * GET /api/security/entries/today
     * Today's entries list
     */
    @GetMapping("/entries/today")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTodayEntries() {
        List<Map<String, Object>> entries = gatePassService.getTodayEntries().stream()
                .map(this::mapGatePassToSummary)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(entries));
    }

    /**
     * GET /api/security/on-leave
     * Students currently outside hostel (exited but not returned)
     */
    @GetMapping("/on-leave")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStudentsOnLeave() {
        List<Map<String, Object>> onLeave = gatePassService.getStudentsCurrentlyOnLeave().stream()
                .map(this::mapGatePassToSummary)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(onLeave));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────
    private Map<String, Object> mapGatePassToSummary(GatePass gp) {
        return Map.of(
                "studentName", gp.getLeaveRequest().getStudent().getUser().getName(),
                "rollNumber", gp.getLeaveRequest().getStudent().getRollNumber(),
                "hostelName", gp.getLeaveRequest().getStudent().getHostelName(),
                "roomNumber", gp.getLeaveRequest().getStudent().getRoomNumber(),
                "leavePassNumber", gp.getLeaveRequest().getLeavePassNumber(),
                "fromDate", gp.getLeaveRequest().getFromDate().toString(),
                "toDate", gp.getLeaveRequest().getToDate().toString(),
                "exitTime", gp.getExitTime() != null ? gp.getExitTime().toString() : "-",
                "entryTime", gp.getEntryTime() != null ? gp.getEntryTime().toString() : "-"
        );
    }

    @Data
    public static class GateAction {
        private String qrToken;
        private String remarks;
    }
}
