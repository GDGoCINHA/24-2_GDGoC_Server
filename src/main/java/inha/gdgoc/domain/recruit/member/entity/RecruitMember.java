package inha.gdgoc.domain.recruit.member.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import inha.gdgoc.domain.recruit.member.enums.AdmissionSemester;
import inha.gdgoc.domain.recruit.member.enums.EnrolledClassification;
import inha.gdgoc.domain.recruit.member.enums.Gender;
import inha.gdgoc.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부원 지원서.
 *
 * <p>학번·전화번호의 유일성은 <b>학기 단위</b>다. 전에는 전역 UNIQUE 라 한 사람이 평생 한 번만
 * 지원할 수 있었다 — 매 학기 회비를 받는 운영과 어긋났다. V20260818 마이그레이션이 복합으로 바꿨다.
 *
 * <p>ddl-auto 는 none 이라 운영 스키마는 마이그레이션이 만든다. 아래 선언은 문서이자
 * 테스트용 H2 스키마의 출처다 — 마이그레이션과 어긋나면 테스트가 실제와 다른 것을 검증하게 된다.
 */
@Entity
@Table(
        name = "recruit_member",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uq_recruit_member_student_id_semester",
                    columnNames = {"student_id", "admission_semester"}),
            @UniqueConstraint(
                    name = "uq_recruit_member_phone_number_semester",
                    columnNames = {"phone_number", "admission_semester"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RecruitMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "enrolled_classification", nullable = false)
    private EnrolledClassification enrolledClassification;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "email", nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
    @Column(name = "birth", nullable = false)
    private LocalDate birth;

    @Column(name = "major", nullable = false)
    private String major;

    @Column(name = "is_payed", nullable = false)
    private Boolean isPayed;

    @Enumerated(EnumType.STRING)
    @Column(name = "admission_semester", nullable = false, length = 10)
    private AdmissionSemester admissionSemester;

    @Builder.Default
    @OneToMany(mappedBy = "recruitMember", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers = new ArrayList<>();

    public void markPaid() {
        this.isPayed = Boolean.TRUE;
    }

    public void markUnpaid() {
        this.isPayed = Boolean.FALSE;
    }
}
