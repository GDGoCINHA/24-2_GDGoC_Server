package inha.gdgoc.domain.study.validator;

import inha.gdgoc.domain.study.entity.Study;
import inha.gdgoc.domain.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class HasAuthorityValidator implements CreateStudyAttendeeValidationRule {

    @Override
    public void validate(User user, Study study) {
        if (user.isGuest()) {
            throw new RuntimeException("사용 권한이 없는 유저입니다.");
        }
    }
}
