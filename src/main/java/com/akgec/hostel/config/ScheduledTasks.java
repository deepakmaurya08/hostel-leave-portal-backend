package com.akgec.hostel.config;

import com.akgec.hostel.repository.ApprovalHistoryRepository;
import com.akgec.hostel.repository.ParentTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final ParentTokenRepository parentTokenRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;

    /**
     * Run every day at 2 AM — clean up expired unused parent tokens
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredTokens() {
        parentTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Expired parent tokens cleaned up");
    }

    @Scheduled(cron = "0 0 3 * * *")  // runs every day at 3 AM
    public void cleanupOldAuditLogs() {
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusDays(10);
        approvalHistoryRepository.deleteAll(
                approvalHistoryRepository.findAll().stream()
                        .filter(h -> h.getTimestamp() != null && h.getTimestamp().isBefore(cutoff))
                        .toList()
        );
        log.info("Old audit logs cleaned up");
    }
}
