package com.akgec.hostel.notification;

import com.akgec.hostel.model.entity.LeaveRequest;
import com.akgec.hostel.model.entity.Student;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${spring.mail.from}")
    private String fromEmail;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ─────────────────────────────────────────────────────────────────────────
    // PARENT APPROVAL EMAIL
    // ─────────────────────────────────────────────────────────────────────────
    @Async
    public void sendParentApprovalEmail(LeaveRequest leave, String approveToken, String rejectToken) {
        try {
            Student student = leave.getStudent();
            Context ctx = new Context();
            ctx.setVariable("studentName", student.getUser().getName());
            ctx.setVariable("rollNumber", student.getRollNumber());
            ctx.setVariable("hostelName", student.getHostelName());
            ctx.setVariable("roomNumber", student.getRoomNumber());
            ctx.setVariable("fromDate", leave.getFromDate().format(DATE_FMT));
            ctx.setVariable("toDate", leave.getToDate().format(DATE_FMT));
            ctx.setVariable("leaveType", leave.getLeaveType().name().replace("_", " "));
            ctx.setVariable("reason", leave.getReason());
            ctx.setVariable("leaveDays", leave.getLeaveDays());
            ctx.setVariable("approveUrl", baseUrl + "/parent/approve?token=" + approveToken);
            ctx.setVariable("rejectUrl", baseUrl + "/parent/reject?token=" + rejectToken);

            String htmlContent = templateEngine.process("parent-approval", ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(student.getParentEmail());
            helper.setSubject("Leave Request Approval Required - " + student.getUser().getName() + " | AKGEC Hostel");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Parent approval email sent to: {}", student.getParentEmail());
        } catch (Exception e) {
            log.error("Failed to send parent approval email for leave {}: {}", leave.getId(), e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STUDENT NOTIFICATIONS
    // ─────────────────────────────────────────────────────────────────────────
    @Async
    public void sendLeaveSubmittedEmail(LeaveRequest leave) {
        try {
            Student student = leave.getStudent();
            String subject = "Leave Request Submitted - " + leave.getLeavePassNumber() + " | AKGEC Hostel";
            String body = buildStudentNotificationHtml(
                    student.getUser().getName(),
                    "Your leave request has been submitted successfully.",
                    "Leave Pass No: <b>" + leave.getLeavePassNumber() + "</b><br>" +
                    "From: <b>" + leave.getFromDate().format(DATE_FMT) + "</b> &nbsp;|&nbsp; " +
                    "To: <b>" + leave.getToDate().format(DATE_FMT) + "</b><br>" +
                    "Status: <b>Pending Parent Approval</b><br><br>" +
                    "An approval email has been sent to your parent/guardian.",
                    frontendUrl + "/student/leaves"
            );
            sendHtmlEmail(student.getUser().getEmail(), subject, body);
        } catch (Exception e) {
            log.error("Failed to send leave submitted email: {}", e.getMessage());
        }
    }

    @Async
    public void sendParentRejectedEmail(LeaveRequest leave) {
        try {
            Student student = leave.getStudent();
            String subject = "Leave Request Rejected by Parent | AKGEC Hostel";
            String body = buildStudentNotificationHtml(
                    student.getUser().getName(),
                    "Your leave request has been rejected by your parent/guardian.",
                    "Leave Pass No: <b>" + leave.getLeavePassNumber() + "</b><br>" +
                    "From: " + leave.getFromDate().format(DATE_FMT) + " To: " + leave.getToDate().format(DATE_FMT),
                    frontendUrl + "/student/leaves"
            );
            sendHtmlEmail(student.getUser().getEmail(), subject, body);
        } catch (Exception e) {
            log.error("Failed to send parent rejected email: {}", e.getMessage());
        }
    }

    @Async
    public void sendWardenRejectedEmail(LeaveRequest leave) {
        try {
            Student student = leave.getStudent();
            String subject = "Leave Request Rejected by Warden | AKGEC Hostel";
            String body = buildStudentNotificationHtml(
                    student.getUser().getName(),
                    "Your leave request has been rejected by the Warden.",
                    "Leave Pass No: <b>" + leave.getLeavePassNumber() + "</b><br>" +
                    "Remarks: " + (leave.getWardenRemarks() != null ? leave.getWardenRemarks() : "No remarks provided"),
                    frontendUrl + "/student/leaves"
            );
            sendHtmlEmail(student.getUser().getEmail(), subject, body);
        } catch (Exception e) {
            log.error("Failed to send warden rejected email: {}", e.getMessage());
        }
    }

    @Async
    public void sendDeanRejectedEmail(LeaveRequest leave) {
        try {
            Student student = leave.getStudent();
            String subject = "Leave Request Rejected by Dean | AKGEC Hostel";
            String body = buildStudentNotificationHtml(
                    student.getUser().getName(),
                    "Your leave request has been rejected by the Dean.",
                    "Leave Pass No: <b>" + leave.getLeavePassNumber() + "</b><br>" +
                    "Remarks: " + (leave.getDeanRemarks() != null ? leave.getDeanRemarks() : "No remarks provided"),
                    frontendUrl + "/student/leaves"
            );
            sendHtmlEmail(student.getUser().getEmail(), subject, body);
        } catch (Exception e) {
            log.error("Failed to send dean rejected email: {}", e.getMessage());
        }
    }

    @Async
    public void sendLeaveApprovedEmail(LeaveRequest leave) {
        try {
            Student student = leave.getStudent();
            String subject = "🎉 Leave APPROVED - " + leave.getLeavePassNumber() + " | AKGEC Hostel";
            String body = buildStudentNotificationHtml(
                    student.getUser().getName(),
                    "Congratulations! Your leave has been fully approved.",
                    "Leave Pass No: <b>" + leave.getLeavePassNumber() + "</b><br>" +
                    "From: <b>" + leave.getFromDate().format(DATE_FMT) + "</b> &nbsp;|&nbsp; " +
                    "To: <b>" + leave.getToDate().format(DATE_FMT) + "</b><br><br>" +
                    "Please download your Leave Pass PDF and QR Code from the portal.<br>" +
                    "Show the QR Code to the Security Guard while exiting and entering the hostel.",
                    frontendUrl + "/student/leaves/" + leave.getId()
            );
            sendHtmlEmail(student.getUser().getEmail(), subject, body);
        } catch (Exception e) {
            log.error("Failed to send leave approved email: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WARDEN NOTIFICATION
    // ─────────────────────────────────────────────────────────────────────────
    @Async
    public void sendWardenNewRequestEmail(LeaveRequest leave, String wardenEmail) {
        try {
            Student student = leave.getStudent();
            String subject = "New Leave Request Pending - " + student.getUser().getName() + " | AKGEC Hostel";
            String body = buildStaffNotificationHtml(
                    "Warden",
                    "A new leave request requires your review.",
                    "Student: <b>" + student.getUser().getName() + "</b> (" + student.getRollNumber() + ")<br>" +
                    "Hostel: " + student.getHostelName() + " | Room: " + student.getRoomNumber() + "<br>" +
                    "Leave: " + leave.getFromDate().format(DATE_FMT) + " to " + leave.getToDate().format(DATE_FMT) + "<br>" +
                    "Reason: " + leave.getReason(),
                    frontendUrl + "/warden/leaves/" + leave.getId()
            );
            sendHtmlEmail(wardenEmail, subject, body);
        } catch (Exception e) {
            log.error("Failed to send warden notification email: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEAN NOTIFICATION
    // ─────────────────────────────────────────────────────────────────────────
    @Async
    public void sendDeanNewRequestEmail(LeaveRequest leave, String deanEmail) {
        try {
            Student student = leave.getStudent();
            String subject = "Leave Request Forwarded for Final Approval - " + student.getUser().getName() + " | AKGEC Hostel";
            String body = buildStaffNotificationHtml(
                    "Dean",
                    "A leave request has been approved by the Warden and requires your final approval.",
                    "Student: <b>" + student.getUser().getName() + "</b> (" + student.getRollNumber() + ")<br>" +
                    "Hostel: " + student.getHostelName() + " | Room: " + student.getRoomNumber() + "<br>" +
                    "Leave: " + leave.getFromDate().format(DATE_FMT) + " to " + leave.getToDate().format(DATE_FMT) + "<br>" +
                    "Days: " + leave.getLeaveDays() + "<br>" +
                    "Warden Remarks: " + (leave.getWardenRemarks() != null ? leave.getWardenRemarks() : "-"),
                    frontendUrl + "/dean/leaves/" + leave.getId()
            );
            sendHtmlEmail(deanEmail, subject, body);
        } catch (Exception e) {
            log.error("Failed to send dean notification email: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private void sendHtmlEmail(String to, String subject, String htmlBody) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(message);
    }

    private String buildStudentNotificationHtml(String name, String headline, String details, String actionUrl) {
        return "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;max-width:600px;margin:auto;'>" +
               "<div style='background:#003366;padding:20px;text-align:center;'>" +
               "<h2 style='color:white;margin:0;'>AKGEC Hostel Leave Portal</h2></div>" +
               "<div style='padding:24px;border:1px solid #e0e0e0;'>" +
               "<p>Dear <b>" + name + "</b>,</p>" +
               "<p style='font-size:16px;color:#003366;'><b>" + headline + "</b></p>" +
               "<div style='background:#f5f5f5;padding:16px;border-radius:6px;margin:16px 0;'>" + details + "</div>" +
               "<div style='text-align:center;margin:24px 0;'>" +
               "<a href='" + actionUrl + "' style='background:#003366;color:white;padding:12px 28px;" +
               "text-decoration:none;border-radius:6px;font-size:14px;'>View on Portal</a></div>" +
               "</div><div style='background:#f0f0f0;padding:12px;text-align:center;font-size:11px;color:#888;'>" +
               "Ajay Kumar Garg Engineering College, Ghaziabad | Hostel Leave Management Portal</div>" +
               "</body></html>";
    }

    private String buildStaffNotificationHtml(String role, String headline, String details, String actionUrl) {
        return "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;max-width:600px;margin:auto;'>" +
               "<div style='background:#003366;padding:20px;text-align:center;'>" +
               "<h2 style='color:white;margin:0;'>AKGEC Hostel Leave Portal</h2></div>" +
               "<div style='padding:24px;border:1px solid #e0e0e0;'>" +
               "<p>Dear <b>" + role + "</b>,</p>" +
               "<p style='font-size:16px;color:#003366;'><b>" + headline + "</b></p>" +
               "<div style='background:#f5f5f5;padding:16px;border-radius:6px;margin:16px 0;'>" + details + "</div>" +
               "<div style='text-align:center;margin:24px 0;'>" +
               "<a href='" + actionUrl + "' style='background:#28a745;color:white;padding:12px 28px;" +
               "text-decoration:none;border-radius:6px;font-size:14px;'>Review Request</a></div>" +
               "</div><div style='background:#f0f0f0;padding:12px;text-align:center;font-size:11px;color:#888;'>" +
               "Ajay Kumar Garg Engineering College, Ghaziabad | Hostel Leave Management Portal</div>" +
               "</body></html>";
    }
}
