package inha.gdgoc.global.security;

import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.security.AccessGuard.AccessCondition;
import inha.gdgoc.global.security.annotation.Authorize;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * {@link Authorize} 어노테이션을 AOP로 처리. 메서드/클래스 진입 전 {@link AccessGuard#require}를 호출한다.
 *
 * <p>메서드 어노테이션이 클래스 어노테이션보다 우선된다.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuthorizeAspect {

  private final AccessGuard accessGuard;

  @Before(
      "@annotation(inha.gdgoc.global.security.annotation.Authorize)"
          + " || (@within(inha.gdgoc.global.security.annotation.Authorize)"
          + "     && !@annotation(inha.gdgoc.global.security.annotation.Authorize))")
  public void enforce(JoinPoint jp) {
    Authorize authorize = resolveAnnotation(jp);

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    CustomUserDetails user =
        (auth != null && auth.getPrincipal() instanceof CustomUserDetails u) ? u : null;

    AccessCondition[] conditions =
        Arrays.stream(authorize.value())
            .map(c -> AccessCondition.of(c.atLeast(), c.teams()))
            .toArray(AccessCondition[]::new);

    accessGuard.require(user, conditions);
  }

  private Authorize resolveAnnotation(JoinPoint jp) {
    MethodSignature sig = (MethodSignature) jp.getSignature();
    Authorize method = sig.getMethod().getAnnotation(Authorize.class);
    return method != null ? method : jp.getTarget().getClass().getAnnotation(Authorize.class);
  }
}
