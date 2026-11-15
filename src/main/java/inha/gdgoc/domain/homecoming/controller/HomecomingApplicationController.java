package inha.gdgoc.domain.homecoming.controller;

import static inha.gdgoc.domain.homecoming.controller.message.HomecomingMessage.APPLICATION_SUBMITTED;

import inha.gdgoc.domain.homecoming.dto.request.HomecomingApplyRequest;
import inha.gdgoc.domain.homecoming.dto.response.HomecomingApplyResponse;
import inha.gdgoc.domain.homecoming.service.HomecomingApplicationService;
import inha.gdgoc.global.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 홈커밍데이 초대장의 신청 제출. 로그인 없이 받는다 — SecurityConfig 에서 이 POST 하나만 열려 있다.
 *
 * <p>조회 API 는 두지 않는다. 신청 화면에서 다른 사람의 신청 내역을 볼 수 있으면 안 된다.
 */
@RestController
@RequestMapping("/api/v1/homecoming")
@RequiredArgsConstructor
public class HomecomingApplicationController {

  private final HomecomingApplicationService applicationService;

  @PostMapping("/applications")
  public ResponseEntity<ApiResponse<HomecomingApplyResponse, Void>> apply(
      @Valid @RequestBody HomecomingApplyRequest req) {
    return ResponseEntity.ok(ApiResponse.ok(APPLICATION_SUBMITTED, applicationService.submit(req)));
  }
}
