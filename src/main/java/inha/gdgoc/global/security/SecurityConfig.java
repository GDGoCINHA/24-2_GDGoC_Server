package inha.gdgoc.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import inha.gdgoc.global.dto.response.ErrorResponse;
import inha.gdgoc.global.exception.GlobalErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@EnableMethodSecurity
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final TokenAuthenticationFilter tokenAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/v1/auth/logout").permitAll()
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/api/v1/auth/**",
                    "/api/v1/test/**",
                    "/api/v1/game/**",
                    "/api/v1/recruit/member/apply/**",
                    "/api/v1/recruit/member/check/**",
                    "/api/v1/recruit/member/memo",
                    "/api/v1/fileupload",
                    "/api/v1/manito/verify")
                .permitAll()
                .anyRequest()
                .authenticated()
            )
            .sessionManagement(
                sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(tokenAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json; charset=UTF-8");

                    ErrorResponse errorResponse = new ErrorResponse(
                        GlobalErrorCode.UNAUTHORIZED_USER
                    );

                    ObjectMapper objectMapper = new ObjectMapper();
                    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                    response.getWriter().flush();
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType("application/json; charset=UTF-8");

                    ErrorResponse errorResponse = new ErrorResponse(
                        GlobalErrorCode.FORBIDDEN_USER
                    );

                    ObjectMapper objectMapper = new ObjectMapper();
                    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                    response.getWriter().flush();
                })
            );

        return http.build();
    }

    /**
     * 애플리케이션 전체 경로에 대한 CORS 설정을 생성하여 등록한다.
     *
     * 생성된 CORS 구성은 지정된 출처 목록, 허용 HTTP 메서드, 허용/노출 헤더, 자격증명 허용 및 프리플라이트 캐시(maxAge)를 포함한다.
     *
     * @return 모든 경로("/**")에 등록된 CorsConfiguration을 제공하는 CorsConfigurationSource 객체
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "https://gdgocinha.com",
                "https://dev.gdgocinha.com",
                "https://www.gdgocinha.com",
                "https://typing-game-alpha-umber.vercel.app",
                "https://api.gdgocinha.com",
                "https://*.gdgocinha.com",
                "https://smpringles24.github.io"
        ));
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS","PATCH"));
        config.setAllowedHeaders(List.of("Origin","X-Requested-With","Content-Type","Accept","Authorization"));
        config.setExposedHeaders(List.of()); // 필요시 노출
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // 프리플라이트 캐시

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
