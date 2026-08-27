package inha.gdgoc.domain.recruit.member.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import inha.gdgoc.domain.recruit.member.entity.RecruitMember;
import inha.gdgoc.domain.recruit.member.enums.AdmissionSemester;
import inha.gdgoc.domain.recruit.member.enums.EnrolledClassification;
import inha.gdgoc.domain.recruit.member.enums.Gender;
import inha.gdgoc.global.util.MajorNormalizer;
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
    private String studentId;
    private String enrolledClassification;
    private String phoneNumber;
    private String email;
    private String gender;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
    private LocalDate birth;
    private String major;
    private Boolean isPayed;

    /**
     * 신원을 로그인한 계정의 값으로 갈아끼운다.
     *
     * <p>폼이 이름·학번·이메일·전화·학과를 보내더라도 무시한다. 회원가입 때 이미 받은 값이고, 지원서와 계정을 잇는 키가
     * 이메일이라 타이핑에 맡기면 오타 하나로 영영 안 이어진다.
     */
    public RecruitMemberRequest withIdentity(
            String name, String studentId, String email, String phoneNumber, String major) {
        return RecruitMemberRequest.builder()
                .name(name)
                .studentId(studentId)
                .email(email)
                .phoneNumber(phoneNumber)
                .major(major)
                .enrolledClassification(this.enrolledClassification)
                .gender(this.gender)
                .birth(this.birth)
                .isPayed(this.isPayed)
                .build();
    }

    public RecruitMember toEntity(AdmissionSemester admissionSemester, MajorNormalizer majorNormalizer) {
        String cleanPhone = phoneNumber.replaceAll("[^0-9]", "");
        return RecruitMember.builder()
                .name(name)
                .studentId(studentId)
                .enrolledClassification(EnrolledClassification.fromStatus(enrolledClassification))
                .phoneNumber(cleanPhone)
                .email(email)
                .gender(Gender.fromType(gender))
                .birth(birth)
                .major(majorNormalizer.normalize(major))
                .isPayed(false)
                .admissionSemester(admissionSemester)
                .build();
    }
}
