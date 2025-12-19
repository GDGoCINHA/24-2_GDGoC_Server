package inha.gdgoc.domain.guestbook.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "guestbook_entry", indexes = {@Index(name = "idx_guestbook_created_at", columnList = "createdAt")})
public class GuestbookEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wristband_serial", nullable = false, unique = true, length = 32)
    private String wristbandSerial; // 손목밴드 일련번호(키)

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, updatable = false)
    private final LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime wonAt; // 당첨 시각 (null이면 미당첨)

    protected GuestbookEntry() {}

    public GuestbookEntry(String wristbandSerial, String name) {
        this.wristbandSerial = wristbandSerial;
        this.name = name;
    }

    public boolean isWon() {return wonAt != null;}

    public void markWon() {this.wonAt = LocalDateTime.now();}
}
