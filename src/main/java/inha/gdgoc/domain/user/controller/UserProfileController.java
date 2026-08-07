package inha.gdgoc.domain.user.controller;

import static inha.gdgoc.domain.user.controller.message.UserProfileMessage.PROFILE_IMAGE_UPDATED_SUCCESS;
import static inha.gdgoc.domain.user.controller.message.UserProfileMessage.PROFILE_RETRIEVED_SUCCESS;
import static inha.gdgoc.domain.user.controller.message.UserProfileMessage.PROFILE_UPDATED_SUCCESS;

import inha.gdgoc.domain.user.dto.request.UpdateUserProfileRequest;
import inha.gdgoc.domain.user.dto.response.UserImageResponse;
import inha.gdgoc.domain.user.dto.response.UserProfileResponse;
import inha.gdgoc.domain.user.service.UserProfileService;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Operation(summary = "내 정보 조회")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse, Void>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails me
    ) {
        UserProfileResponse response = userProfileService.getMyProfile(me.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(PROFILE_RETRIEVED_SUCCESS, response));
    }

    @Operation(summary = "내 정보 수정")
    @PreAuthorize("isAuthenticated()")
    @PatchMapping
    public ResponseEntity<ApiResponse<UserProfileResponse, Void>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails me,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        UserProfileResponse response = userProfileService.updateMyProfile(me.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(PROFILE_UPDATED_SUCCESS, response));
    }

    @Operation(summary = "프로필 이미지 변경")
    @PreAuthorize("isAuthenticated()")
    @PatchMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserImageResponse, Void>> updateMyImage(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestPart("file") MultipartFile file
    ) {
        UserImageResponse response = userProfileService.updateMyImage(me.getUserId(), file);
        return ResponseEntity.ok(ApiResponse.ok(PROFILE_IMAGE_UPDATED_SUCCESS, response));
    }
}
