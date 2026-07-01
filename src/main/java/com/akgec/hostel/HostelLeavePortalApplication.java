package com.akgec.hostel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class HostelLeavePortalApplication {
    public static void main(String[] args) {
        SpringApplication.run(HostelLeavePortalApplication.class, args);
    }
}
