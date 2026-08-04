package inha.gdgoc.global.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드 또는 클래스에 접근 가능한 최소 권한 조건을 선언한다.
 *
 * <p>{@link Condition}을 OR 관계로 여러 개 나열할 수 있다. 하나라도 충족하면 통과.
 *
 * <pre>{@code
 * // ORGANIZER 이상
 * @Authorize(@Condition(atLeast = UserRole.ORGANIZER))
 *
 * // LEAD 이상 OR CORE + PR팀
 * @Authorize({
 *     @Condition(atLeast = UserRole.LEAD),
 *     @Condition(atLeast = UserRole.CORE, teams = TeamType.PR_DESIGN)
 * })
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Authorize {

  Condition[] value();
}
