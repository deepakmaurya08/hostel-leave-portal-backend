package com.akgec.hostel.dto.response;

import com.akgec.hostel.model.enums.ApprovalAction;
import com.akgec.hostel.model.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApprovalHistoryResponse {
    private Long id;
    private String performedBy;
    private Role role;
    private ApprovalAction action;
    private String remarks;
    private LocalDateTime timestamp;
}
