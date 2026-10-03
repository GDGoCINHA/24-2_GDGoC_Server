package inha.gdgoc.domain.club.activity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * 활동 기록 제출·수정. 수정도 화면 전체를 다시 보내므로 같은 형태를 쓴다.
 *
 * @param scheduleId 사전 등록한 일정에 연결할 때만. 없으면 일정 없이 진행한 활동이다
 * @param attendedUserIds 출석한 사람만. 명단은 서버가 활동일로 정하며, 명단 밖 id 는 무시한다
 */
public record ClubActivitySubmitRequest(
    @NotNull LocalDate activityDate,
    Long scheduleId,
    @NotEmpty @Size(max = 10) List<@NotBlank @Size(max = 500) String> photoUrls,
    @NotBlank String content,
    List<Long> attendedUserIds,
    String progressNote) {}
