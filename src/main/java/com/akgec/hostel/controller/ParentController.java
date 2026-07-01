package com.akgec.hostel.controller;

import com.akgec.hostel.service.impl.ParentApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/parent")
@RequiredArgsConstructor
public class ParentController {

    private final ParentApprovalService parentApprovalService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * GET /api/parent/approve?token=xyz
     * Parent clicks approve link from email - redirects to frontend confirmation page
     */
    @GetMapping("/approve")
    public ResponseEntity<Void> approveLeave(@RequestParam String token) {
        String result = parentApprovalService.approveLeave(token);
        // Redirect to frontend with result
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(frontendUrl + "/parent/response?action=approved&status=" + result));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    /**
     * GET /api/parent/reject?token=xyz
     * Parent clicks reject link from email - redirects to frontend confirmation page
     */
    @GetMapping("/reject")
    public ResponseEntity<Void> rejectLeave(@RequestParam String token) {
        String result = parentApprovalService.rejectLeave(token);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(frontendUrl + "/parent/response?action=rejected&status=" + result));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    /**
     * POST /api/parent/approve (API-based, for mobile/frontend integration)
     */
    @PostMapping("/approve")
    public ResponseEntity<String> approveLeaveApi(@RequestParam String token) {
        String result = parentApprovalService.approveLeave(token);
        return ResponseEntity.ok("Leave " + result + " successfully.");
    }

    /**
     * POST /api/parent/reject (API-based)
     */
    @PostMapping("/reject")
    public ResponseEntity<String> rejectLeaveApi(@RequestParam String token) {
        String result = parentApprovalService.rejectLeave(token);
        return ResponseEntity.ok("Leave " + result + " successfully.");
    }
}
