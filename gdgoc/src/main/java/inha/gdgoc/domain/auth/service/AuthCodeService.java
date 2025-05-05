package inha.gdgoc.domain.auth.service;

import inha.gdgoc.domain.auth.entity.AuthCode;
import inha.gdgoc.domain.auth.repository.AuthCodeRepository;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthCodeService {

    private final AuthCodeRepository authCodeRepository;
    private final Duration EXPIRATION = Duration.ofMinutes(5);

    @Transactional
    public void saveAuthCode(String email, String code) {
        authCodeRepository.deleteByEmail(email);
        authCodeRepository.save(new AuthCode(email, code));
    }

    @Transactional
    public boolean verify(String email, String code) {
        Optional<AuthCode> optional = authCodeRepository.findByEmail(email);

        if (optional.isEmpty()) return false;

        AuthCode authCode = optional.get();
        if (authCode.isExpired(EXPIRATION)) {
            authCodeRepository.deleteByEmail(email);
            return false;
        }

        boolean result = authCode.matches(code);
        authCodeRepository.deleteByEmail(email);
        return result;
    }
}
