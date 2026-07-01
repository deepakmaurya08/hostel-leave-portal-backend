package com.akgec.hostel.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WardenDashboardResponse {
    private long pendingRequests;
    private long approvedToday;
    private long rejectedToday;
    private long escalatedToDean;
    private List<LeaveResponse> pendingLeaves;
}
