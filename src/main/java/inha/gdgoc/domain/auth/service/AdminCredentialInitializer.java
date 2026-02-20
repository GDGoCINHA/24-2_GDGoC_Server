package inha.gdgoc.domain.auth.service;

import inha.gdgoc.domain.auth.entity.AdminCredential;
import inha.gdgoc.domain.auth.repository.AdminCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdminCredentialInitializer {

    private final AdminCredentialRepository adminCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.login-id:}")
    private String adminLoginId;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Bean
    public ApplicationRunner initAdminCredentialRunner() {
        return args -> initializeIfConfigured();
    }

    @Transactional
    protected void initializeIfConfigured() {
        if (!StringUtils.hasText(adminLoginId) || !StringUtils.hasText(adminPassword)) {
            log.info("Admin credential initialization skipped (app.admin.login-id/password not configured).");
            return;
        }

        String encodedPassword = passwordEncoder.encode(adminPassword);
        adminCredentialRepository.findByLoginId(adminLoginId.trim()).ifPresentOrElse(
                credential -> {
                    credential.updatePasswordHash(encodedPassword);
                    credential.updateEnabled(true);
                },
                () -> adminCredentialRepository.save(AdminCredential.builder()
                        .loginId(adminLoginId.trim())
                        .passwordHash(encodedPassword)
                        .enabled(true)
                        .build())
        );

        log.info("Admin credential initialized for loginId={}", adminLoginId.trim());
    }
}
