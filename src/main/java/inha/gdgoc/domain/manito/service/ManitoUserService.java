package inha.gdgoc.domain.manito.service;

import inha.gdgoc.domain.manito.entity.ManitoAssignment;
import inha.gdgoc.domain.manito.entity.ManitoSession;
import inha.gdgoc.domain.manito.repository.ManitoAssignmentRepository;
import inha.gdgoc.domain.manito.repository.ManitoSessionRepository;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ManitoUserService {

    private final ManitoSessionRepository sessionRepository;
    private final ManitoAssignmentRepository assignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final ManitoPinPolicy manitoPinPolicy;   // ✅ PIN 정책 주입

    /**
     * pin 검증 후 암호문 반환
     */
    public String verifyAndGetCipher(String sessionCode, String studentId, String pinPlain) {

        ManitoSession session = sessionRepository.findByCode(sessionCode)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND, "세션 코드가 올바르지 않습니다."));

        ManitoAssignment assignment = assignmentRepository.findBySessionAndStudentId(session, studentId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND, "해당 학번은 세션에 참여하지 않았습니다."));

        // ✅ Admin 쪽과 동일한 규칙으로 PIN 정규화
        String normalizedPin = manitoPinPolicy.normalize(pinPlain);

        if (normalizedPin.isEmpty()) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "PIN 형식이 올바르지 않습니다.");
        }

        if (!passwordEncoder.matches(normalizedPin, assignment.getPinHash())) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER, "PIN이 일치하지 않습니다.");
        }

        if (assignment.getEncryptedManitto() == null || assignment.getEncryptedManitto().isBlank()) {
            throw new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND, "아직 마니또 암호문이 업로드되지 않았습니다. 관리자에게 문의하세요.");
        }

        return assignment.getEncryptedManitto();
    }
}