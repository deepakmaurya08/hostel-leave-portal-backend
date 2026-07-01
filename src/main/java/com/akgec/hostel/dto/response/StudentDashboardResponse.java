package com.akgec.hostel.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StudentDashboardResponse {
    private String studentName;
    private String rollNumber;
    private String hostelName;
    private String roomNumber;
    private long totalLeavesTaken;
    private long pendingRequests;
    private long approvedLeaves;
    private long rejectedLeaves;
    private List<LeaveResponse> recentLeaves;
}
