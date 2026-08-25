package inha.gdgoc.domain.landing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import inha.gdgoc.domain.landing.dto.LandingContentPayload;
import inha.gdgoc.domain.landing.entity.LandingContent;
import inha.gdgoc.domain.landing.enums.LandingContentStatus;
import inha.gdgoc.domain.landing.exception.LandingErrorCode;
import inha.gdgoc.domain.landing.repository.LandingContentRepository;
import inha.gdgoc.global.exception.BusinessException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 랜딩 콘텐츠를 읽고 쓴다.
 *
 * <p>문서는 검증을 통과한 뒤 JSON 문자열로 저장한다. 컨트롤러가 {@code @Valid} 로 이미 걸러내므로
 * 저장 경로에 들어오는 문서는 사진 주소와 길이가 모두 검증된 것이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LandingContentService {

    private final LandingContentRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * 방문자에게 보일 문서. 발행된 게 없으면 비어 있다.
     *
     * <p>비어 있음을 오류로 만들지 않는다 — 웹이 번들에 든 기본값을 그대로 쓰면 되고, 그게 아직 아무것도
     * 발행하지 않은 정상 상태다.
     */
    @Transactional(readOnly = true)
    public Optional<LandingContentPayload> findPublished() {
        return read(LandingContentStatus.PUBLISHED);
    }

    /**
     * 관리자가 편집 중인 문서.
     *
     * <p>초안이 없으면 발행본을 준다. 처음 편집을 시작할 때 빈 화면 대신 지금 나가고 있는 내용에서
     * 이어 고치게 하려는 것이다. 둘 다 없으면 비어 있고, 그때는 웹이 번들 기본값을 채워 보낸다.
     */
    @Transactional(readOnly = true)
    public Optional<LandingContentPayload> findDraft() {
        return read(LandingContentStatus.DRAFT).or(this::findPublished);
    }

    @Transactional
    public void saveDraft(LandingContentPayload payload, Long updatedBy) {
        write(LandingContentStatus.DRAFT, serialize(payload), updatedBy);
    }

    /**
     * 초안을 발행본으로 옮긴다.
     *
     * <p>초안이 없으면 막는다. 발행본을 지우거나 빈 문서로 덮어쓰는 사고를 내지 않기 위해서다.
     */
    @Transactional
    public void publish(Long updatedBy) {
        String draft =
            repository
                .findByStatus(LandingContentStatus.DRAFT)
                .map(LandingContent::getContent)
                .orElseThrow(() -> new BusinessException(LandingErrorCode.LANDING_DRAFT_NOT_FOUND));

        write(LandingContentStatus.PUBLISHED, draft, updatedBy);
    }

    private Optional<LandingContentPayload> read(LandingContentStatus status) {
        return repository.findByStatus(status).map(row -> deserialize(row.getContent(), status));
    }

    private void write(LandingContentStatus status, String content, Long updatedBy) {
        LandingContent row =
            repository
                .findByStatus(status)
                .orElseGet(() -> LandingContent.create(status, content, updatedBy));
        row.apply(content, updatedBy);
        repository.save(row);
    }

    private String serialize(LandingContentPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BusinessException(LandingErrorCode.LANDING_CONTENT_NOT_WRITABLE);
        }
    }

    /**
     * 저장된 문서를 되읽는다.
     *
     * <p>여기서 깨지는 경우는 하나뿐이다 — 문서 모양을 바꾼 뒤 예전 문서가 남아 있을 때. 그때 방문자에게
     * 500 을 주는 대신 비어 있는 것으로 다루고, 웹은 번들 기본값을 보여준다. 대신 로그를 남긴다.
     */
    private LandingContentPayload deserialize(String content, LandingContentStatus status) {
        try {
            return objectMapper.readValue(content, LandingContentPayload.class);
        } catch (JsonProcessingException e) {
            log.error("[landing] 저장된 콘텐츠를 읽지 못했다 - status={}", status, e);
            return null;
        }
    }
}
