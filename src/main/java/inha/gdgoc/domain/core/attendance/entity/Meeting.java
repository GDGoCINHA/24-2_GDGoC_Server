package inha.gdgoc.domain.core.attendance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "meetings")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Meeting {

    /** 단일 날짜 = 단일 회의 정책 → 날짜를 PK로 */
    @Id
    @Column(name = "meeting_date", nullable = false)
    private LocalDate meetingDate;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}