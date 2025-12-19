package inha.gdgoc.domain.guestbook.dto.response;

import inha.gdgoc.domain.guestbook.entity.GuestbookEntry;

import java.time.LocalDateTime;

public record GuestbookEntryResponse(Long id, String wristbandSerial, String name, LocalDateTime createdAt,
                                     LocalDateTime wonAt) {

    public static GuestbookEntryResponse from(GuestbookEntry e) {
        return new GuestbookEntryResponse(e.getId(), e.getWristbandSerial(), e.getName(), e.getCreatedAt(), e.getWonAt());
    }
}
