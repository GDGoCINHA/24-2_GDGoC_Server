package inha.gdgoc.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import org.junit.jupiter.api.Test;

class UserProfileUpdateTest {

    @Test
    void updateProfile_changesOnlyEditableFields() {
        User user = createUser();

        user.updateProfile("김철수", "CSE", "01099998888");

        assertThat(user.getName()).isEqualTo("김철수");
        assertThat(user.getMajor()).isEqualTo("CSE");
        assertThat(user.getPhoneNumber()).isEqualTo("01099998888");
        assertThat(user.getStudentId()).isEqualTo("12201234");
        assertThat(user.getEmail()).isEqualTo("hong@inha.edu");
        assertThat(user.getUserRole()).isEqualTo(UserRole.CORE);
    }

    @Test
    void updateImage_replacesImageUrl() {
        User user = createUser();

        user.updateImage("https://bucket.s3.amazonaws.com/user/1/profile/abc.png");

        assertThat(user.getImage())
                .isEqualTo("https://bucket.s3.amazonaws.com/user/1/profile/abc.png");
    }

    private User createUser() {
        return User.builder()
                .name("홍길동")
                .major("DTE")
                .studentId("12201234")
                .phoneNumber("01012345678")
                .email("hong@inha.edu")
                .userRole(UserRole.CORE)
                .team(TeamType.TECH)
                .image(null)
                .social(null)
                .careers(null)
                .build();
    }
}
