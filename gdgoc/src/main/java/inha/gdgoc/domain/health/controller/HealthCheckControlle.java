package inha.gdgoc.domain.health.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class HealthCheckController {

    @GetMapping
    public String healthCheck() {
        return "OK"; // ALB의 헬스체크가 정상적으로 응답할 수 있도록 문자열 반환
    }
}
