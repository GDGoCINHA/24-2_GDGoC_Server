package inha.gdgoc.domain.study.validator;

import inha.gdgoc.domain.study.entity.Study;
import inha.gdgoc.domain.user.entity.User;

public interface CreateStudyAttendeeValidationRule {
    void validate(User user, Study study);
}
