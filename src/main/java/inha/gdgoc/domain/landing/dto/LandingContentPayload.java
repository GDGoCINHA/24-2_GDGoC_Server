package inha.gdgoc.domain.landing.dto;

import inha.gdgoc.domain.landing.enums.LandingBadgeTone;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
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
    @Valid @NotNull LandingAbout about,
    @Valid @NotNull @Size(max = 6) List<LandingPhoto> photoStrip,
    @Valid @NotNull @Size(max = 12) List<LandingActivity> activities,
    @Valid @NotNull LandingHackathonIntro hackathonIntro,
    @Valid @NotNull @Size(max = 12) List<LandingHackathon> hackathons,
    @Valid @NotNull @Size(max = 12) List<LandingFaq> faqs,
    @Valid @NotNull LandingContact contact,
    /** 히어로 배지에 붙는 학기 표기. 예: {@code 2026-2}. */
    @NotBlank @Size(max = 20) String semesterLabel) {

  /**
   * 사진 주소로 허용하는 모양.
   *
   * <p>{@code /images/} 로 시작하는 번들 사진이거나 {@code https://} 로 시작하는 업로드 사진만 받는다.
   * {@code javascript:}·{@code data:} 를 막기 위한 것이고, 공백과 따옴표·꺾쇠도 함께 막아 둔다.
   */
  private static final String PHOTO_SRC = "^(/images/|https://)[^\\s\"'<>]{1,480}$";

  /** 바깥으로 나가는 링크. 사진과 달리 번들 경로가 없어 https 로 시작하는 것만 받는다. */
  private static final String LINK_URL = "^https://[^\\s\"'<>]{1,280}$";

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

  /**
   * 커뮤니티 소개.
   *
   * <p>가치 네 칸은 개수가 고정이다. 번호와 색이 GDG 4색에 1:1 로 묶여 있어서 개수가 바뀌면 색이
   * 어긋난다. 그래서 여기서는 문구만 받고 번호·색은 웹이 자리 순서로 붙인다 — 배지 색을 이름으로만
   * 받는 것과 같은 이유다.
   */
  public record LandingAbout(
      /** 두 줄로 끊어 쓴다. 화면에서 첫 줄만 흐린 색으로 그린다. */
      @NotNull @Size(min = 2, max = 2) List<@NotBlank @Size(max = 120) String> heading,
      @NotBlank @Size(max = 300) String body,
      @Valid @NotNull @Size(min = 4, max = 4) List<LandingAboutValue> values) {}

  public record LandingAboutValue(
      @NotBlank @Size(max = 20) String title, @NotBlank @Size(max = 160) String body) {}

  /**
   * 문의 창구. 바닥글과 FAQ 답변이 같이 쓴다.
   *
   * <p>오픈채팅 주소는 https 로 시작하는 것만 받는다. 로그인 없이 보는 첫 화면에 그대로 링크로
   * 걸리므로 {@code javascript:} 를 막아야 한다 — 사진 주소와 같은 이유다.
   */
  public record LandingContact(
      @NotBlank @Email @Size(max = 120) String email,
      @NotBlank
          @Size(max = 300)
          @Pattern(regexp = LINK_URL, message = "링크는 https:// 로 시작해야 합니다.")
          String openChatUrl) {}

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
