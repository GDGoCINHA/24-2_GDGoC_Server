package inha.gdgoc.domain.auth.dto.request;

import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSignupRequest {
    private String name;
    private String major;
    private String studentId;
    private String phoneNumber;
    private String email;
    private String password;

    public User toEntity(String hashedPassword, byte[] salt) {
        return User.builder()
                .name(name)
                .major(major)
                .studentId(studentId)
                .phoneNumber(phoneNumber)
                .email(email)
                .password(hashedPassword)
                .salt(salt)
                .userRole(UserRole.GUEST)
                .build();
    }
}
