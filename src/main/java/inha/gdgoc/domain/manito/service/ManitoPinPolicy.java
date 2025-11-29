package inha.gdgoc.domain.manito.service;

import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import org.springframework.stereotype.Component;

@Component
public class ManitoPinPolicy {
    public String normalize(String rawPin) {
        if (rawPin == null) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "PIN 값이 비어 있습니다.");
        }

        // 숫자만 추출
        String digits = rawPin.replaceAll("\\D", "");

        if (digits.isEmpty()) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "PIN 값에는 적어도 1자리 이상의 숫자가 있어야 합니다.");
        }

        // 4자리 zero-padding
        try {
            int asInt = Integer.parseInt(digits);
            return String.format("%04d", asInt);
        } catch (NumberFormatException e) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "PIN 형식이 올바르지 않습니다.");
        }
    }
}