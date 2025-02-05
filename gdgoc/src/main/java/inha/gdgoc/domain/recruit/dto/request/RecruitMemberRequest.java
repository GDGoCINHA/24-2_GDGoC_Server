package inha.gdgoc.domain.recruit.dto.request;

import inha.gdgoc.domain.recruit.entity.RecruitMember;
import inha.gdgoc.domain.recruit.enums.EnrolledClassification;
import inha.gdgoc.domain.recruit.enums.Gender;
import inha.gdgoc.domain.recruit.enums.Nationality;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class RecruitMemberRequest {
    private String name;
    private String grade;
    private String studentId;
    private String enrolledClassification;
    private String phoneNumber;
    private String nationality;
    private String nationalityContent;
    private String email;
    private String gender;
    private LocalDate birth; // 다시 확인
    private String major;
    private String doubleMajor;
    private boolean isPayed;

    public RecruitMember toEntity() {
        return RecruitMember.builder()
                .name(name)
                .grade(grade)
                .studentId(studentId)
                .enrolledClassification(EnrolledClassification.fromStatus(enrolledClassification))
                .phoneNumber(phoneNumber)
                .nationality(Nationality.fromNation(nationality))
                .nationalityContent(nationalityContent)
                .email(email)
                .gender(Gender.fromType(gender))
                .birth(birth)
                .major(major)
                .doubleMajor(doubleMajor)
                .isPayed(isPayed)
                .build();
    }
}
