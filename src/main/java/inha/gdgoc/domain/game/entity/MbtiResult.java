package inha.gdgoc.domain.game.entity;

import inha.gdgoc.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "mbti_result",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_mbti_result_name_student_id", columnNames = {"name", "student_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Builder
public class MbtiResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "student_id", nullable = false, length = 20)
    private String studentId;

    @Column(name = "mbti_type", nullable = false, length = 4)
    private String mbtiType;

    public void updateMbtiType(String mbtiType) {
        this.mbtiType = mbtiType;
    }
}
