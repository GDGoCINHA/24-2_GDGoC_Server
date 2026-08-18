package inha.gdgoc.domain.recruit.member.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import inha.gdgoc.domain.recruit.member.entity.Answer;
import inha.gdgoc.domain.recruit.member.entity.RecruitMember;
import inha.gdgoc.domain.recruit.member.enums.AdmissionSemester;
import java.util.List;
import java.time.Instant;

public record SpecifiedMemberResponse(
        Long id,
        String name,
        String enrolledClassification,
        String phoneNumber,
        String email,
        String gender,
        String birth,
        String major,
        String studentId,
        AdmissionSemester admissionSemester,
        boolean isPayed,
        Instant createdAt,
        Instant updatedAt,
        AnswersResponse answers
) {

    public static SpecifiedMemberResponse from(
            RecruitMember member,
            List<Answer> answers,
            ObjectMapper objectMapper
    ) {
        return new SpecifiedMemberResponse(
                member.getId(),
                member.getName(),
                member.getEnrolledClassification() != null ? member.getEnrolledClassification().name() : null,
                member.getPhoneNumber(),
                member.getEmail(),
                member.getGender() != null ? member.getGender().name() : null,
                member.getBirth() != null ? member.getBirth().toString() : null,
                member.getMajor(),
                member.getStudentId(),
                member.getAdmissionSemester(),
                Boolean.TRUE.equals(member.getIsPayed()),
                member.getCreatedAt(),
                member.getUpdatedAt(),
                AnswersResponse.from(answers, objectMapper)
        );
    }
}
