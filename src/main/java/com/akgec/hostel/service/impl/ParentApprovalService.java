package com.akgec.hostel.service.impl;

import com.akgec.hostel.exception.HostelException;
import com.akgec.hostel.model.entity.LeaveRequest;
import com.akgec.hostel.model.entity.ParentToken;
import com.akgec.hostel.model.enums.ApprovalAction;
import com.akgec.hostel.model.enums.LeaveStatus;
import com.akgec.hostel.model.enums.Role;
import com.akgec.hostel.notification.EmailNotificationService;
import com.akgec.hostel.repository.LeaveRequestRepository;
import com.akgec.hostel.repository.ParentTokenRepository;
import com.akgec.hostel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParentApprovalService {

    private final ParentTokenRepository parentTokenRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final EmailNotificationService emailService;
    private final LeaveService leaveService;

    @Transactional
    public String approveLeave(String token) {
        ParentToken parentToken = validateToken(token);

        LeaveRequest leave = parentToken.getLeaveRequest();

        if (leave.getStatus() != LeaveStatus.PENDING_PARENT) {
            parentToken.setUsed(true);
            parentTokenRepository.save(parentToken);
            return "This leave request has already been processed. Current status: " + leave.getStatus().name();
        }

        // Mark token used
        parentToken.setUsed(true);
        parentToken.setAction("APPROVED");
        parentTokenRepository.save(parentToken);

        // Invalidate all other tokens for this leave
        invalidateOtherTokens(leave.getId(), token);

        // Update leave status
        leave.setStatus(LeaveStatus.PENDING_WARDEN);
        leave.setParentApprovedAt(LocalDateTime.now());
        leaveRequestRepository.save(leave);

        // Record history
        leaveService.recordHistory(leave, "PARENT:" + leave.getStudent().getParentEmail(),
                Role.ROLE_PARENT, ApprovalAction.PARENT_APPROVED, "Approved via email link");



        log.info("Leave {} approved by parent", leave.getLeavePassNumber());
        return "approved";
    }

    @Transactional
    public String rejectLeave(String token) {
        ParentToken parentToken = validateToken(token);

        LeaveRequest leave = parentToken.getLeaveRequest();

        if (leave.getStatus() != LeaveStatus.PENDING_PARENT) {
            parentToken.setUsed(true);
            parentTokenRepository.save(parentToken);
            return "This leave request has already been processed. Current status: " + leave.getStatus().name();
        }

        parentToken.setUsed(true);
        parentToken.setAction("REJECTED");
        parentTokenRepository.save(parentToken);

        invalidateOtherTokens(leave.getId(), token);

        leave.setStatus(LeaveStatus.PARENT_REJECTED);
        leaveRequestRepository.save(leave);

        leaveService.recordHistory(leave, "PARENT:" + leave.getStudent().getParentEmail(),
                Role.ROLE_PARENT, ApprovalAction.PARENT_REJECTED, "Rejected via email link");

        emailService.sendParentRejectedEmail(leave);

        log.info("Leave {} rejected by parent", leave.getLeavePassNumber());
        return "rejected";
    }

    private ParentToken validateToken(String token) {
        ParentToken parentToken = parentTokenRepository.findByToken(token)
                .orElseThrow(() -> new HostelException.ResourceNotFoundException("Invalid approval link"));

        if (parentToken.isExpired()) {
            throw new HostelException.TokenExpiredException(
                    "This approval link has expired. Please ask the student to re-apply or contact the hostel office.");
        }
        if (parentToken.isUsed()) {
            throw new HostelException.InvalidRequestException(
                    "This approval link has already been used.");
        }
        return parentToken;
    }

    private void invalidateOtherTokens(Long leaveId, String usedToken) {
        parentTokenRepository.findAll().stream()
                .filter(t -> t.getLeaveRequest().getId().equals(leaveId)
                        && !t.getToken().equals(usedToken)
                        && !t.isUsed())
                .forEach(t -> {
                    t.setUsed(true);
                    parentTokenRepository.save(t);
                });
    }
}
