package com.akgec.hostel.dto.request;

import com.akgec.hostel.model.enums.LeaveType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class LeaveApplicationRequest {

    @NotNull(message = "Leave type is required")
    private LeaveType leaveType;

    // Period of Leave - form field 4
    @NotNull(message = "From date is required")
    @FutureOrPresent(message = "From date must be today or future")
    private LocalDate fromDate;

    @NotNull(message = "To date is required")
    @Future(message = "To date must be in future")
    private LocalDate toDate;

    // Time Out - form field 4
    private LocalTime timeOut;

    // Reason for Leave (State Clearly) - form field 5
    @NotBlank(message = "Reason is required")
    private String reason;

    // Name of the Person being visited - form field 6
    private String visitPersonName;

    // Relation - form field 6
    private String visitPersonRelation;

    // Address and Contact No of person being visited - form field 7
    private String visitPersonAddress;
    private String visitPersonContact;

    // Emergency contact override
    private String emergencyContact;

    // Attendance details (filled by department)
    private Double attendancePercentage;
    private String hodName;
}
