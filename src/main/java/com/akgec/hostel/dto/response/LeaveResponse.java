package com.akgec.hostel.dto.response;

import com.akgec.hostel.model.enums.LeaveStatus;
import com.akgec.hostel.model.enums.LeaveType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class LeaveResponse {
    private Long id;
    private String leavePassNumber;

    // Student info
    private String studentName;
    private String rollNumber;
    private String studentNo;
    private String hostelName;
    private String roomNumber;
    private String courseBranch;

    // Leave details
    private LeaveType leaveType;
    private String reason;
    private LocalDate fromDate;
    private LocalDate toDate;
    private LocalTime timeOut;
    private long leaveDays;

    // Visit details (from form fields 6 & 7)
    private String visitPersonName;
    private String visitPersonRelation;
    private String visitPersonAddress;
    private String visitPersonContact;

    // Attendance (form attendance table)
    private Double attendancePercentage;
    private String hodName;

    // Approval info
    private LeaveStatus status;
    private String wardenRemarks;
    private String deanRemarks;
    private Integer workingDaysCount;
    private String wardenParentCommTime;
    private String approvedByWarden;
    private String approvedByDean;

    // Timestamps
    private LocalDateTime parentApprovedAt;
    private LocalDateTime wardenApprovedAt;
    private LocalDateTime deanApprovedAt;
    private LocalDateTime createdAt;

    // Gate pass info
    private GatePassInfo gatePass;

    // Timeline
    private List<ApprovalHistoryResponse> timeline;

    private List<String> documentUrls;

    @Data
    @Builder
    public static class GatePassInfo {
        private String qrToken;
        private LocalDateTime exitTime;
        private LocalDateTime entryTime;
        private boolean pdfReady;
    }
}
