package inha.gdgoc.domain.recruit.member.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import inha.gdgoc.domain.recruit.member.entity.RecruitMember;
import inha.gdgoc.domain.recruit.member.enums.AdmissionSemester;
import inha.gdgoc.domain.recruit.member.enums.EnrolledClassification;
import inha.gdgoc.domain.recruit.member.enums.Gender;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
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
    private String email;
    private String gender;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
    private LocalDate birth;
    private String major;
    private Boolean isPayed;

    public RecruitMember toEntity(AdmissionSemester admissionSemester) {
        String cleanPhone = phoneNumber.replaceAll("[^0-9]", "");
        return RecruitMember.builder()
                .name(name)
                .grade(grade)
                .studentId(studentId)
                .enrolledClassification(EnrolledClassification.fromStatus(enrolledClassification))
                .phoneNumber(cleanPhone)
                .nationality(nationality)
                .email(email)
                .gender(Gender.fromType(gender))
                .birth(birth)
                .major(major)
                .isPayed(false)
                .admissionSemester(admissionSemester)
                .build();
    }
}
