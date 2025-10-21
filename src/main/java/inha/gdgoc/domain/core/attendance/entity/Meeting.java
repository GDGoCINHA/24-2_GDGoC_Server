package inha.gdgoc.domain.core.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "meetings", uniqueConstraints = {@UniqueConstraint(name = "uq_meeting_date", columnNames = "meeting_date")}, indexes = {@Index(name = "idx_meeting_date", columnList = "meeting_date")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 도메인 식별자: 날짜는 유니크로 강제
     */
    @Column(name = "meeting_date", nullable = false)
    private LocalDate meetingDate;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}