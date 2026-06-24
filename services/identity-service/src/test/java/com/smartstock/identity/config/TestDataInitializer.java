package com.smartstock.identity.config;

import com.smartstock.identity.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("test")
@RequiredArgsConstructor
public class TestDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Test admin password - used ONLY in the test profile
    static final String TEST_ADMIN_PASSWORD = "Admin@SmartStock2026!";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        userRepository.findByIdAndNotDeleted("user-system-admin").ifPresent(admin -> {
            admin.setPasswordHash(passwordEncoder.encode(TEST_ADMIN_PASSWORD));
            userRepository.save(admin);
            log.info("Test admin password initialized for test profile");
        });
    }
}
