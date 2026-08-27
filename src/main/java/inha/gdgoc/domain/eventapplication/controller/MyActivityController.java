package inha.gdgoc.domain.eventapplication.controller;

import static inha.gdgoc.domain.eventapplication.controller.message.EventApplicationMessage.ACTIVITIES_RETRIEVED;

import inha.gdgoc.domain.eventapplication.dto.response.MyActivityResponse;
import inha.gdgoc.domain.eventapplication.service.MyActivityService;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.security.annotation.Authorize;
import inha.gdgoc.global.security.annotation.Condition;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@Authorize(@Condition(atLeast = UserRole.GUEST))
public class MyActivityController {

  private final MyActivityService myActivityService;

  @GetMapping("/activities")
  public ResponseEntity<ApiResponse<List<MyActivityResponse>, Void>> listMyActivities(
      @AuthenticationPrincipal CustomUserDetails me) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            ACTIVITIES_RETRIEVED, myActivityService.listMyEventActivities(me.getUserId())));
  }
}
