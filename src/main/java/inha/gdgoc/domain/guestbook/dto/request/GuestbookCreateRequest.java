package inha.gdgoc.domain.guestbook.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuestbookCreateRequest(@NotBlank @Size(max = 32) String wristbandSerial,
                                     @NotBlank @Size(max = 50) String name) {

}
