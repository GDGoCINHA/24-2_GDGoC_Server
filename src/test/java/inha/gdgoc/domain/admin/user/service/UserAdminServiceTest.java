package inha.gdgoc.domain.admin.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import inha.gdgoc.domain.admin.user.dto.request.UpdateUserRoleTeamRequest;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserAdminService userAdminService;

    @Test
    void updateRoleAndTeam_blocksSelfEdit() {
        User editor = createUser(1L, UserRole.CORE, TeamType.HR);
        when(userRepository.findById(1L)).thenReturn(Optional.of(editor));

        assertThatThrownBy(() -> userAdminService.updateRoleAndTeam(
                principal(1L, UserRole.CORE, TeamType.HR),
                1L,
                new UpdateUserRoleTeamRequest(UserRole.MEMBER, null)
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void updateRoleAndTeam_allowsHrCoreToPromoteGuestToMember() {
        User editor = createUser(1L, UserRole.CORE, TeamType.HR);
        User target = createUser(2L, UserRole.GUEST, TeamType.TECH);

        when(userRepository.findById(1L)).thenReturn(Optional.of(editor));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        userAdminService.updateRoleAndTeam(
                principal(1L, UserRole.CORE, TeamType.HR),
                2L,
                new UpdateUserRoleTeamRequest(UserRole.MEMBER, null)
        );

        assertThat(target.getUserRole()).isEqualTo(UserRole.MEMBER);
        assertThat(target.getTeam()).isNull();
        verify(userRepository).save(target);
    }

    @Test
    void updateRoleAndTeam_blocksNonHrCoreEditingOtherTeam() {
        User editor = createUser(1L, UserRole.CORE, TeamType.TECH);
        User target = createUser(2L, UserRole.GUEST, TeamType.BD);

        when(userRepository.findById(1L)).thenReturn(Optional.of(editor));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> userAdminService.updateRoleAndTeam(
                principal(1L, UserRole.CORE, TeamType.TECH),
                2L,
                new UpdateUserRoleTeamRequest(UserRole.MEMBER, null)
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void updateRoleAndTeam_allowsNonHrLeadMemberToCoreInSameTeam() {
        User editor = createUser(1L, UserRole.LEAD, TeamType.TECH);
        User target = createUser(2L, UserRole.MEMBER, TeamType.TECH);

        when(userRepository.findById(1L)).thenReturn(Optional.of(editor));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        userAdminService.updateRoleAndTeam(
                principal(1L, UserRole.LEAD, TeamType.TECH),
                2L,
                new UpdateUserRoleTeamRequest(UserRole.CORE, TeamType.TECH)
        );

        assertThat(target.getUserRole()).isEqualTo(UserRole.CORE);
        assertThat(target.getTeam()).isEqualTo(TeamType.TECH);
        verify(userRepository).save(target);
    }

    private CustomUserDetails principal(Long userId, UserRole role, TeamType team) {
        return new CustomUserDetails(
                userId,
                "test@inha.edu",
                "session",
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())),
                role,
                team
        );
    }

    private User createUser(Long id, UserRole role, TeamType team) {
        User user = User.builder()
                .name("홍길동")
                .major("컴퓨터공학과")
                .studentId("12201234")
                .phoneNumber("01012345678")
                .email("hong@inha.edu")
                .userRole(role)
                .team(team)
                .image(null)
                .social(null)
                .careers(null)
                .build();
        setId(user, id);
        return user;
    }

    private void setId(Object target, Long id) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
