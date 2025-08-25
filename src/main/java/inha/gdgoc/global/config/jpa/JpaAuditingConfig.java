package inha.gdgoc.global.config.jpa;

import java.time.Instant;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing(dateTimeProviderRef = "utcDateTimeProvider")
@Configuration
public class JpaAuditingConfig {

    @Bean
    public DateTimeProvider utcDateTimeProvider() {
        return () -> Optional.of(Instant.now());
    }
}
