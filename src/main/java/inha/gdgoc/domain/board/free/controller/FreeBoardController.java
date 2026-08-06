package inha.gdgoc.domain.board.free.controller;

import static inha.gdgoc.domain.board.free.controller.message.FreeBoardMessage.*;

import inha.gdgoc.domain.board.common.enums.SearchType;
import inha.gdgoc.domain.board.free.dto.request.FreeBoardCreateRequest;
import inha.gdgoc.domain.board.free.dto.request.FreeBoardUpdateRequest;
import inha.gdgoc.domain.board.free.dto.response.FreeBoardDetailResponse;
import inha.gdgoc.domain.board.free.dto.response.FreeBoardSummaryResponse;
import inha.gdgoc.domain.board.free.service.FreeBoardService;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.dto.response.PageMeta;
import inha.gdgoc.global.security.annotation.Authorize;
import inha.gdgoc.global.security.annotation.Condition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 자유게시판.
 *
 * <p>조회는 회원 전용이다 — SecurityConfig 의 permitAll 에 이 경로를 넣지 않아 anyRequest().authenticated()
 * 가 받는다. 행사 게시판만 공개다.
 *
 * <p>작성은 MEMBER 이상이다. 공지·행사가 CORE 이상인 것과 다르다. 수정·삭제는 여기서 MEMBER 로 한 번 거르고,
 * '작성자 본인 또는 ORGANIZER 이상'인지는 서비스가 판정한다 — 본인 여부는 어노테이션으로 표현할 수 없다.
 */
@RestController
@RequestMapping("/api/v1/board/free")
@RequiredArgsConstructor
@Validated
public class FreeBoardController {

  private final FreeBoardService freeBoardService;

  @GetMapping
  public ResponseEntity<ApiResponse<Page<FreeBoardSummaryResponse>, PageMeta>> listPosts(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "15") @Min(1) @Max(100) int size,
      @RequestParam(defaultValue = "TITLE_AND_CONTENT") SearchType searchType,
      @RequestParam(required = false) String keyword) {

    Page<FreeBoardSummaryResponse> posts =
        freeBoardService.listPosts(page, size, searchType, keyword);

    return ResponseEntity.ok(ApiResponse.ok(FREE_LIST_RETRIEVED, posts, PageMeta.of(posts)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<FreeBoardDetailResponse, Void>> getPost(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(FREE_RETRIEVED, freeBoardService.getPost(id)));
  }

  @Authorize(@Condition(atLeast = UserRole.MEMBER))
  @PostMapping
  public ResponseEntity<ApiResponse<Long, Void>> createPost(
      @AuthenticationPrincipal CustomUserDetails me,
      @Valid @RequestBody FreeBoardCreateRequest req) {
    return ResponseEntity.ok(
        ApiResponse.ok(FREE_CREATED, freeBoardService.createPost(req, me.getUserId())));
  }

  @Authorize(@Condition(atLeast = UserRole.MEMBER))
  @PatchMapping("/{id}")
  public ResponseEntity<ApiResponse<Void, Void>> updatePost(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long id,
      @Valid @RequestBody FreeBoardUpdateRequest req) {
    freeBoardService.updatePost(id, req, me.getUserId(), me.getRole());
    return ResponseEntity.ok(ApiResponse.ok(FREE_UPDATED));
  }

  @Authorize(@Condition(atLeast = UserRole.MEMBER))
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void, Void>> deletePost(
      @AuthenticationPrincipal CustomUserDetails me, @PathVariable Long id) {
    freeBoardService.deletePost(id, me.getUserId(), me.getRole());
    return ResponseEntity.ok(ApiResponse.ok(FREE_DELETED));
  }
}
