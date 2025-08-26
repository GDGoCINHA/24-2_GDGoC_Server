package inha.gdgoc.domain.recruit.dto.request;

import inha.gdgoc.domain.recruit.entity.RecruitMember;
import inha.gdgoc.domain.recruit.enums.EnrolledClassification;
import inha.gdgoc.domain.recruit.enums.Gender;
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
    private LocalDate birth;
    private String major;
    private String doubleMajor;
    private Boolean isPayed;

    /**
     * DTO의 필드 값을 바탕으로 RecruitMember 엔티티를 생성하여 반환합니다.
     *
     * EnrolledClassification과 Gender는 문자열을 변환하는 팩토리 메서드를 통해 매핑되며,
     * 생성된 엔티티의 isPayed 필드는 항상 false로 설정됩니다(요청의 isPayed 값은 사용되지 않음).
     *
     * 주의: EnrolledClassification.fromStatus 및 Gender.fromType 호출은 입력 문자열이 유효하지 않을 경우 예외를 던질 수 있습니다.
     *
     * @return 변환된 RecruitMember 엔티티
     */
    public RecruitMember toEntity() {
        return RecruitMember.builder()
                .name(name)
                .grade(grade)
                .studentId(studentId)
                .enrolledClassification(EnrolledClassification.fromStatus(enrolledClassification))
                .phoneNumber(phoneNumber)
                .nationality(nationality)
                .email(email)
                .gender(Gender.fromType(gender))
                .birth(birth)
                .major(major)
                .doubleMajor(doubleMajor)
                .isPayed(false)
                .build();
    }
}
