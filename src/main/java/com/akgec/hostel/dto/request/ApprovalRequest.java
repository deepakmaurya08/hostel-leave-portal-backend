package com.akgec.hostel.dto.request;

import lombok.Data;

@Data
public class ApprovalRequest {
    private String remarks;
    private Integer workingDaysCount;        // Warden's Remarks field from form
    private String wardenParentCommTime;     // Communication with Parents/Time from form
}
