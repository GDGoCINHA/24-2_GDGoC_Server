package inha.gdgoc.domain.auth.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthCodeService {

    private static class AuthCodeInfo {
        private final String code;
        private final LocalDateTime issuedAt;

        AuthCodeInfo(String code, LocalDateTime issuedAt) {
            this.code = code;
            this.issuedAt = issuedAt;
        }

        boolean isExpired(Duration expiration) {
            return issuedAt.plus(expiration).isBefore(LocalDateTime.now());
        }

        boolean matches(String inputCode) {
            return code.equals(inputCode);
        }
    }

    private final Map<String, AuthCodeInfo> codeStorage = new ConcurrentHashMap<>();
    private final Duration EXPIRATION = Duration.ofMinutes(5);

    public void saveAuthCode(String email, String code) {
        codeStorage.put(email, new AuthCodeInfo(code, LocalDateTime.now()));
    }

    public boolean verify(String email, String code) {
        AuthCodeInfo info = codeStorage.get(email);
        if (info == null) return false;
        if (info.isExpired(EXPIRATION)) {
            codeStorage.remove(email); // 만료된 건 제거
            return false;
        }
        boolean response = info.matches(code);
        codeStorage.remove(email);
        return response;
    }
}

