package inha.gdgoc.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("!test")
@Component
public class DotenvLoader {

    @PostConstruct
    public void loadEnv() {
        try {
            Dotenv dotenv = Dotenv.load();
            System.setProperty("AWS_ACCESS_KEY_ID", dotenv.get("AWS_ACCESS_KEY_ID"));
            System.setProperty("AWS_SECRET_ACCESS_KEY", dotenv.get("AWS_SECRET_ACCESS_KEY"));
            System.setProperty("AWS_REGION", dotenv.get("AWS_REGION"));
            System.setProperty("AWS_RESOURCE_BUCKET", dotenv.get("AWS_RESOURCE_BUCKET"));
            System.setProperty("AWS_TEST_RESOURCE_BUCKET", dotenv.get("AWS_TEST_RESOURCE_BUCKET"));
            System.setProperty("GOOGLE_CLIENT_ID", dotenv.get("GOOGLE_CLIENT_ID"));
            System.setProperty("GOOGLE_CLIENT_SECRET", dotenv.get("GOOGLE_CLIENT_SECRET"));
            System.setProperty("GOOGLE_REDIRECT_URI", dotenv.get("GOOGLE_REDIRECT_URI"));
            System.setProperty("GMAIL", dotenv.get("GMAIL"));
            System.setProperty("GMAIL_PASSWORD", dotenv.get("GMAIL_PASSWORD"));
        } catch (Exception e) {
            e.printStackTrace();  // 오류 로그 출력
            throw new RuntimeException("Error loading .env file: " + e.getMessage(), e);
        }
    }
}