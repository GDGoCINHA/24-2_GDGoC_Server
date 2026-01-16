package inha.gdgoc.domain.auth.dto.request;
import lombok.Data;
@Data public class SignupRequest {
    private String oauthSubject;
    private String email;
    private String name;
    private String studentId;
    private String phoneNumber;
    private String major;
}