// src/main/java/inha/gdgoc/domain/core/attendance/entity/AttendanceRecord.java
package inha.gdgoc.domain.core.attendance.entity;

import inha.gdgoc.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "attendance_records",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_attendance", columnNames = {"meeting_date", "user_id"})
        },
        indexes = {
                @Index(name = "idx_attendance_user_date", columnList = "user_id, meeting_date DESC"),
                @Index(name = "idx_attendance_date_only", columnList = "meeting_date DESC")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → meetings(meeting_date) */
    @Column(name = "meeting_date", nullable = false)
    private LocalDate meetingDate;

    /** FK → users(id) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_attendance_user"))
    private User user;

    @Column(name = "present", nullable = false)
    private boolean present;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** 마지막 수정자(선택) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by",
            foreignKey = @ForeignKey(name = "fk_attendance_updated_by"))
    private User updatedBy;

    @PrePersist @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}