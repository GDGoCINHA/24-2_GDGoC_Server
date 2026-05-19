package inha.gdgoc.global.security.annotation;

import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** AccessCondition 하나를 표현. {@link Authorize#value()}의 요소 타입으로만 사용. */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Condition {

  UserRole atLeast();

  TeamType[] teams() default {};
}
