package inha.gdgoc.domain.landing.dto;

import inha.gdgoc.domain.landing.enums.LandingBadgeTone;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 온보딩(랜딩)에 실리는 콘텐츠.
 *
 * <p>웹의 {@code src/constant/landingContent.ts} 와 같은 모양이다. 한쪽을 바꾸면 반대쪽도 같은 작업에서
 * 고쳐야 한다 — 자동 생성 파이프라인이 없어 빌드가 알려주지 않는다.
 *
 * <p>구조가 아니라 **내용**만 담는다. 화면 배치는 웹이 갖는다.
 *
 * <p>불투명한 JSON 을 그대로 받지 않고 타입을 박아 둔 이유는 검증이다. 이 문서는 로그인 없이 보는 첫 화면에
 * 그대로 그려지므로, 사진 주소가 {@code javascript:} 로 시작하거나 문자열이 무한정 길어지면 안 된다.
 */
public record LandingContentPayload(
    @Valid @NotNull LandingHero hero,
    @Valid @NotNull @Size(max = 6) List<LandingPhoto> photoStrip,
    @Valid @NotNull @Size(max = 12) List<LandingActivity> activities,
    @Valid @NotNull LandingHackathonIntro hackathonIntro,
    @Valid @NotNull @Size(max = 12) List<LandingHackathon> hackathons,
    @Valid @NotNull @Size(max = 12) List<LandingFaq> faqs) {

  /**
   * 사진 주소로 허용하는 모양.
   *
   * <p>{@code /images/} 로 시작하는 번들 사진이거나 {@code https://} 로 시작하는 업로드 사진만 받는다.
   * {@code javascript:}·{@code data:} 를 막기 위한 것이고, 공백과 따옴표·꺾쇠도 함께 막아 둔다.
   */
  private static final String PHOTO_SRC = "^(/images/|https://)[^\\s\"'<>]{1,480}$";

  public record LandingPhoto(
      @NotBlank @Size(max = 500) @Pattern(regexp = PHOTO_SRC, message = "사진 주소는 /images/ 또는 https:// 로 시작해야 합니다.")
          String src,
      /** 스크린리더용. 사진을 교체하면 반드시 함께 고친다. */
      @NotBlank @Size(max = 200) String alt,
      @Size(max = 120) String caption,
      /** {@code object-position} 의 세로 초점(%). 얼굴이 잘리는 사진을 배포 없이 맞추기 위한 값이다. */
      @Min(0) @Max(100) int focusY) {}

  public record LandingHero(
      @Valid @NotNull LandingPhoto photo,
      /** 제목은 두 조각으로 나눠 뒷부분만 강조 자간을 쓴다. */
      @NotBlank @Size(max = 120) String titleLead,
      @NotBlank @Size(max = 60) String titleAccent,
      @Size(max = 30) String titleTail,
      @Size(max = 200) String description,
      @Size(max = 120) String ctaNote) {}

  public record LandingActivity(
      @NotBlank @Size(max = 60) String title, @NotBlank @Size(max = 200) String body) {}

  public record LandingHackathonIntro(
      @NotBlank @Size(max = 120) String heading,
      @Size(max = 300) String body,
      @Valid @NotNull LandingPhoto photo) {}

  public record LandingHackathon(
      /** 연도. 아직 안 정해졌으면 '—'. */
      @NotBlank @Size(max = 10) String year,
      @NotBlank @Size(max = 120) String title,
      @NotBlank @Size(max = 30) String badge,
      /**
       * 배지 색. 자유 문자열이 아니라 정해진 값 중 하나다.
       *
       * <p>웹은 Tailwind 클래스로 색을 내는데, DB 에만 있는 클래스는 스캐너가 못 찾아 CSS 가 생성되지
       * 않는다. 관리자가 클래스를 직접 적게 두면 저장은 되고 색은 안 나온다.
       */
      @NotNull LandingBadgeTone badgeTone,
      @NotBlank @Size(max = 400) String body) {}

  public record LandingFaq(
      @NotBlank @Size(max = 200) String question,
      /** 문단 배열. 줄바꿈이 아니라 문단으로 나눠 답변 안에서 목록처럼 읽히게 한다. */
      @NotNull @Size(max = 12) List<@NotBlank @Size(max = 500) String> answer) {}
}
