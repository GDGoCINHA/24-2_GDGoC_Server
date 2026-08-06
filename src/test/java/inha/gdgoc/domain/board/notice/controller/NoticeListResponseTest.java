package inha.gdgoc.domain.board.notice.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import inha.gdgoc.domain.board.notice.dto.response.NoticeListResponse;
import inha.gdgoc.domain.board.notice.dto.response.NoticeSummaryResponse;
import inha.gdgoc.domain.board.notice.enums.NoticeCategory;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/** 고정 3건은 size=15 와 별개 필드다. */
class NoticeListResponseTest {

  @Test
  @DisplayName("고정과 일반 목록이 서로 다른 필드에 담긴다")
  void pinnedAndPostsAreSeparate() {
    NoticeSummaryResponse pinned = summary(1L, "고정 공지");
    Page<NoticeSummaryResponse> posts =
        new PageImpl<>(List.of(summary(2L, "일반 공지")), PageRequest.of(0, 15), 137);

    NoticeListResponse response = new NoticeListResponse(List.of(pinned), posts);

    assertThat(response.pinned()).extracting(NoticeSummaryResponse::title).containsExactly("고정 공지");
    assertThat(response.posts().getContent())
        .extracting(NoticeSummaryResponse::title)
        .containsExactly("일반 공지");
  }

  @Test
  @DisplayName("전체 건수는 일반 글 기준이며 고정 3건을 포함하지 않는다")
  void totalElementsCountsPostsOnly() {
    Page<NoticeSummaryResponse> posts =
        new PageImpl<>(List.of(summary(2L, "일반")), PageRequest.of(0, 15), 137);

    NoticeListResponse response = new NoticeListResponse(List.of(summary(1L, "고정")), posts);

    assertThat(response.posts().getTotalElements()).isEqualTo(137);
  }

  @Test
  @DisplayName("고정된 글이 일반 목록에도 나올 수 있다")
  void pinnedNoticeMayAlsoAppearInPosts() {
    NoticeSummaryResponse same = summary(1L, "같은 글");
    Page<NoticeSummaryResponse> posts =
        new PageImpl<>(List.of(same), PageRequest.of(0, 15), 1);

    NoticeListResponse response = new NoticeListResponse(List.of(same), posts);

    assertThat(response.pinned().get(0).id()).isEqualTo(response.posts().getContent().get(0).id());
  }

  @Test
  @DisplayName("boolean 필드는 record 컴포넌트 이름 그대로 직렬화된다")
  void booleanFieldKeepsRecordComponentName() throws Exception {
    // Web 은 타입을 손으로 관리한다. 이 JSON 키가 곧 계약이며, 바뀌면 프론트가 조용히 undefined 를
    // 읽는다 — 빌드도 테스트도 알려주지 않는다. 그래서 이름 자체를 단언한다.
    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    String json = mapper.writeValueAsString(summary(1L, "공지"));

    assertThat(json).contains("\"isPublished\"");
    assertThat(json).doesNotContain("\"published\":");
  }

  private NoticeSummaryResponse summary(Long id, String title) {
    return new NoticeSummaryResponse(
        id, NoticeCategory.OPERATION, title, "홍길동", 0, true, Instant.EPOCH);
  }
}
