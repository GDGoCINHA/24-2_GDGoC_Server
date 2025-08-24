package inha.gdgoc.domain.study.validator;

import inha.gdgoc.domain.study.entity.Study;
import inha.gdgoc.domain.user.entity.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateStudyAttendeeValidator {
    private final List<CreateStudyAttendeeValidationRule> rules;

    public void validateAll(User user, Study study) {
        for (CreateStudyAttendeeValidationRule rule : rules) {
            rule.validate(user, study);
        }
    }
}
