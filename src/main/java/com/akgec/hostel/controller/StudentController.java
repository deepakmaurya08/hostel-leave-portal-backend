package com.akgec.hostel.controller;

import com.akgec.hostel.dto.request.LeaveApplicationRequest;
import com.akgec.hostel.dto.response.ApiResponse;
import com.akgec.hostel.dto.response.LeaveResponse;
import com.akgec.hostel.dto.response.StudentDashboardResponse;
import com.akgec.hostel.service.impl.GatePassService;
import com.akgec.hostel.service.impl.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.util.List;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    private final LeaveService leaveService;
    private final GatePassService gatePassService;

    /**
     * GET /api/student/dashboard
     * Returns student dashboard with leave counts and recent leaves
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<StudentDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(leaveService.getStudentDashboard()));
    }

    /**
     * POST /api/student/leave/apply
     * Apply for leave - multipart form with documents
     */
    @PostMapping(value = "/leave/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LeaveResponse>> applyLeave(
            @Valid @RequestPart("leave") LeaveApplicationRequest request,
            @RequestPart(value = "documents", required = false) List<MultipartFile> documents) {
        LeaveResponse response = leaveService.applyLeave(request, documents);
        return ResponseEntity.ok(ApiResponse.success(response, "Leave application submitted successfully. Parent approval email sent."));
    }

    /**
     * GET /api/student/leaves
     * Get all leaves for logged-in student
     */
    @GetMapping("/leaves")
    public ResponseEntity<ApiResponse<List<LeaveResponse>>> getMyLeaves() {
        return ResponseEntity.ok(ApiResponse.success(leaveService.getMyLeaves()));
    }

    /**
     * GET /api/student/leaves/{leaveId}
     * Get single leave with full timeline
     */
    @GetMapping("/leaves/{leaveId}")
    public ResponseEntity<ApiResponse<LeaveResponse>> getLeave(@PathVariable Long leaveId) {
        return ResponseEntity.ok(ApiResponse.success(leaveService.getLeaveById(leaveId)));
    }

    /**
     * DELETE /api/student/leaves/{leaveId}/cancel
     * Cancel a pending leave request
     */
    @DeleteMapping("/leaves/{leaveId}/cancel")
    public ResponseEntity<ApiResponse<LeaveResponse>> cancelLeave(@PathVariable Long leaveId) {
        LeaveResponse response = leaveService.cancelLeave(leaveId);
        return ResponseEntity.ok(ApiResponse.success(response, "Leave cancelled successfully"));
    }

    /**
     * GET /api/student/leaves/{leaveId}/download-pdf
     * Download approved leave pass PDF
     */
    @GetMapping("/leaves/{leaveId}/download-pdf")
    public ResponseEntity<Resource> downloadPdf(@PathVariable Long leaveId) {
        Resource resource = gatePassService.downloadPdf(leaveId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"leave_pass_" + leaveId + ".pdf\"")
                .body(resource);
    }
}
