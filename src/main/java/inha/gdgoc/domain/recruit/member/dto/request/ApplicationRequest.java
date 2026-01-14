package inha.gdgoc.domain.recruit.member.dto.request;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ApplicationRequest {
    private RecruitMemberRequest member;
    private Map<String, Object> answers;
}
