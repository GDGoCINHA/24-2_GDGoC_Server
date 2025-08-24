package inha.gdgoc.config.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties("jwt")
public class JwtProperties {
    private String selfIssuer;  // 자체 로그인 발급자
    private String googleIssuer;  // 구글 로그인 발급자
    private String secretKey;
}
