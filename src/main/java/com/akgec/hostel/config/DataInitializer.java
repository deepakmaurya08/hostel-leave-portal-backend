package com.akgec.hostel.config;

import com.akgec.hostel.model.entity.User;
import com.akgec.hostel.model.enums.Role;
import com.akgec.hostel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${default.admin.name}")
    private String adminName;

    @Value("${default.admin.email}")
    private String adminEmail;

    @Value("${default.admin.password}")
    private String adminPassword;

    @Value("${default.warden.name}")
    private String wardenName;

    @Value("${default.warden.email}")
    private String wardenEmail;

    @Value("${default.warden.password}")
    private String wardenPassword;

    @Value("${default.dean.name}")
    private String deanName;

    @Value("${default.dean.email}")
    private String deanEmail;

    @Value("${default.dean.password}")
    private String deanPassword;

    @Value("${default.security.name}")
    private String securityName;

    @Value("${default.security.email}")
    private String securityEmail;

    @Value("${default.security.password}")
    private String securityPassword;

    @Override
    public void run(String... args) {

        createUserIfNotExists(adminName, adminEmail, adminPassword, Role.ROLE_ADMIN);
        createUserIfNotExists(wardenName, wardenEmail, wardenPassword, Role.ROLE_WARDEN);
        createUserIfNotExists(deanName, deanEmail, deanPassword, Role.ROLE_DEAN);
        createUserIfNotExists(securityName, securityEmail, securityPassword, Role.ROLE_SECURITY);

        log.info("Default users initialized.");
    }

    private void createUserIfNotExists(String name, String email, String password, Role role) {
        if (!userRepository.existsByEmail(email)) {
            User user = User.builder()
                    .name(name)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .role(role)
                    .active(true)
                    .build();

            userRepository.save(user);
            log.info("Created default {} user: {}", role, email);
        }
    }
}