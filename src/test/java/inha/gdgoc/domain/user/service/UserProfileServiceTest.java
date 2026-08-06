package inha.gdgoc.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import inha.gdgoc.domain.user.dto.request.UpdateUserProfileRequest;
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

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.major()).isEqualTo("DTE");
        assertThat(response.studentId()).isEqualTo("12201234");
        assertThat(response.phoneNumber()).isEqualTo("01012345678");
        assertThat(response.email()).isEqualTo("hong@inha.edu");
        assertThat(response.userRole()).isEqualTo(UserRole.CORE);
        assertThat(response.team()).isEqualTo(TeamType.TECH);
        assertThat(response.membershipStatus()).isEqualTo(User.MembershipStatus.PENDING);
        assertThat(response.image()).isNull();
    }

    @Test
    void getMyProfile_throwsWhenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getMyProfile(99L))
                .isInstanceOf(UserException.class);
    }

    @Test
    void updateMyProfile_updatesEditableFieldsAndNormalizesMajorLabel() {
        User user = createUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = userProfileService.updateMyProfile(
                1L,
                new UpdateUserProfileRequest("김철수", "컴퓨터공학과", "01099998888")
        );

        assertThat(response.name()).isEqualTo("김철수");
        assertThat(response.major()).isEqualTo("CSE");
        assertThat(response.phoneNumber()).isEqualTo("01099998888");
    }

    @Test
    void updateMyProfile_acceptsMajorCodeDirectly() {
        User user = createUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = userProfileService.updateMyProfile(
                1L,
                new UpdateUserProfileRequest("홍길동", "CSE", "01012345678")
        );

        assertThat(response.major()).isEqualTo("CSE");
    }

    @Test
    void updateMyProfile_rejectsUnknownMajor() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser()));

        assertThatThrownBy(() -> userProfileService.updateMyProfile(
                1L,
                new UpdateUserProfileRequest("홍길동", "없는학과", "01012345678")
        )).isInstanceOf(UserException.class);
    }

    @Test
    void updateMyProfile_rejectsHyphenatedPhoneNumber() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(createUser()));

        assertThatThrownBy(() -> userProfileService.updateMyProfile(
                1L,
                new UpdateUserProfileRequest("홍길동", "DTE", "010-1234-5678")
        )).isInstanceOf(UserException.class);
    }

    @Test
    void updateMyProfile_accepts10DigitPhoneNumber() {
        User user = createUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = userProfileService.updateMyProfile(
                1L,
                new UpdateUserProfileRequest("홍길동", "DTE", "0111234567")
        );

        assertThat(response.phoneNumber()).isEqualTo("0111234567");
    }

    @Test
    void updateMyProfile_doesNotTouchStudentIdOrEmail() {
        User user = createUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userProfileService.updateMyProfile(
                1L,
                new UpdateUserProfileRequest("김철수", "CSE", "01099998888")
        );

        assertThat(user.getStudentId()).isEqualTo("12201234");
        assertThat(user.getEmail()).isEqualTo("hong@inha.edu");
    }

    static User createUser() {
        User user = User.builder()
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
        setId(user, 1L);
        return user;
    }

    private static void setId(Object target, Long id) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
