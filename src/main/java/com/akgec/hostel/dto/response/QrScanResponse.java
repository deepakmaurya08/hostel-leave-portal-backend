package com.akgec.hostel.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class QrScanResponse {
    private boolean valid;
    private String message;

    // Student info shown to guard
    private String studentName;
    private String rollNumber;
    private String studentNo;
    private String hostelName;
    private String roomNumber;
    private String photoUrl;

    // Leave info
    private String leavePassNumber;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String leaveType;
    private String reason;

    // Gate pass state
    private boolean exitDone;
    private boolean entryDone;
    private LocalDateTime exitTime;
    private LocalDateTime entryTime;

    // Approval chain
    private boolean parentApproved;
    private boolean wardenApproved;
    private boolean deanApproved;

    // Action available
    private String nextAction; // "MARK_EXIT" or "MARK_ENTRY" or "COMPLETED" or "INVALID"
}
