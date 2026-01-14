package inha.gdgoc.domain.recruit.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties("recruit.core")
public class RecruitCoreProperties {

    /**
     * 현재 모집 회차 (예: 2026-1). 환경 설정에서 주입된다.
     */
    private String session;

    public String currentSession() {
        if (session == null || session.isBlank()) {
            throw new IllegalStateException("recruit.core.session 값이 설정되지 않았습니다.");
        }
        return session;
    }
}
