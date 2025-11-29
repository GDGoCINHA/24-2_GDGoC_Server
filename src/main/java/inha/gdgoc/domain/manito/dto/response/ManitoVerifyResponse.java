package inha.gdgoc.domain.manito.dto.response;

/**
 * 마니또 검증 성공 시, 클라이언트에서 복호화할 암호문과
 * 요청자(=giver)의 이름을 내려주는 DTO
 */
public record ManitoVerifyResponse(
        String encryptedManito,
        String ownerName
) {
}