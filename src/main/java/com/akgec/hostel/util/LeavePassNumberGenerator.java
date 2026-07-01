package com.akgec.hostel.util;

import com.akgec.hostel.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class LeavePassNumberGenerator {

    private final LeaveRequestRepository leaveRequestRepository;

    public String generate() {
        long count = leaveRequestRepository.count() + 1;
        int year = Year.now().getValue();
        return String.format("LV-%d-%08d", year, count);
    }
}
