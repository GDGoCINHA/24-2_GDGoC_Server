package inha.gdgoc.domain.study.validator;

import inha.gdgoc.domain.study.entity.Study;
import inha.gdgoc.domain.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class IsCreatedByMeValidator implements CreateStudyAttendeeValidationRule {

    @Override
    public void validate(User user, Study study) {
        if (study.isCreatedBy(user.getId())) {
            throw new RuntimeException("자신이 만든 스터디에는 가입할 수 없습니다.");
        }
    }
}
