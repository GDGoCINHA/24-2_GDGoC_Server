package inha.gdgoc.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class DotenvLoader {

    @PostConstruct
    public void loadEnv() {
        try {
            Dotenv dotenv = Dotenv.load();
            System.setProperty("GOOGLE_CLIENT_ID", dotenv.get("GOOGLE_CLIENT_ID"));
            System.setProperty("GOOGLE_CLIENT_SECRET", dotenv.get("GOOGLE_CLIENT_SECRET"));
            System.setProperty("GOOGLE_REDIRECT_URI", dotenv.get("GOOGLE_REDIRECT_URI"));
        } catch (Exception e) {
            e.printStackTrace();  // 오류 로그 출력
            throw new RuntimeException("Error loading .env file: " + e.getMessage(), e);
        }
    }
}