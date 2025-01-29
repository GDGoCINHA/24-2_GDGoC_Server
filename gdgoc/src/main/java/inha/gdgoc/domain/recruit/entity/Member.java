package inha.gdgoc.domain.recruit.entity;

import inha.gdgoc.domain.recruit.enums.EnrolledClassification;
import inha.gdgoc.domain.recruit.enums.Gender;
import inha.gdgoc.domain.recruit.enums.Nationality;
import inha.gdgoc.global.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "grade", nullable = false)
    private int grade;

    @Column(name = "student_id", nullable = false, unique = true)
    private int studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "enrolled_classification", nullable = false)
    private EnrolledClassification enrolledClassification;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "nationality", nullable = false)
    private Nationality nationality;

    @Column(name = "nationality_content", nullable = true)
    private String nationalityContent;

    @Column(name = "email", nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Column(name = "birth", nullable = false)
    private LocalDate birth;

    @Column(name = "school", nullable = false)
    private String school;

    @Column(name = "major", nullable = false)
    private String major;

    @Column(name = "double_major", nullable = true)
    private String doubleMajor;

    @Column(name = "route", nullable = false)
    private String route;

    @Column(name = "is_payed", nullable = false)
    private boolean isPayed;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers = new ArrayList<>();
}
