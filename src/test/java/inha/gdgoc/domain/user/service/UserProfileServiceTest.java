package inha.gdgoc.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import inha.gdgoc.domain.user.dto.response.UserProfileResponse;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.exception.UserException;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.domain.resource.service.S3Service;
import inha.gdgoc.global.util.MajorNormalizer;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private S3Service s3Service;

    @Spy
    private MajorNormalizer majorNormalizer = new MajorNormalizer();

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    void getMyProfile_returnsAllProfileFields() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser()));

        UserProfileResponse response = userProfileService.getMyProfile(1L);

        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.major()).isEqualTo("DTE");
        assertThat(response.studentId()).isEqualTo("12201234");
        assertThat(response.phoneNumber()).isEqualTo("01012345678");
        assertThat(response.email()).isEqualTo("hong@inha.edu");
        assertThat(response.userRole()).isEqualTo(UserRole.CORE);
        assertThat(response.team()).isEqualTo(TeamType.TECH);
    }

    @Test
    void getMyProfile_throwsWhenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getMyProfile(99L))
                .isInstanceOf(UserException.class);
    }

    static User createUser() {
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
