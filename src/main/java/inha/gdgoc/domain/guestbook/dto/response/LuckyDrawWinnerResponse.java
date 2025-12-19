package inha.gdgoc.domain.guestbook.dto.response;

import inha.gdgoc.domain.guestbook.entity.GuestbookEntry;

import java.time.LocalDateTime;

public record LuckyDrawWinnerResponse(Long id, String wristbandSerial, String name, LocalDateTime wonAt) {

    public static LuckyDrawWinnerResponse from(GuestbookEntry e) {
        return new LuckyDrawWinnerResponse(e.getId(), e.getWristbandSerial(), e.getName(), e.getWonAt());
    }
}
