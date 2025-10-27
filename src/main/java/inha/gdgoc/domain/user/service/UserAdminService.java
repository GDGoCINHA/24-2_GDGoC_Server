package inha.gdgoc.domain.user.service;

import inha.gdgoc.domain.user.dto.request.UpdateUserRoleTeamRequest;
import inha.gdgoc.domain.user.dto.response.UserSummaryResponse;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;

    /* ======================= 목록 ======================= */

    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> listUsers(String q, Pageable pageable) {
        Pageable fixed = rewriteSort(pageable);
        return userRepository.findSummaries(q, fixed);
    }

    private Pageable rewriteSort(Pageable pageable) {
        Sort original = pageable.getSort();
        if (original.isUnsorted()) return pageable;

        Sort composed = Sort.unsorted();
        boolean hasUserRoleOrder = false;

        for (Sort.Order o : original) {
            String prop = o.getProperty();
            Sort.Direction dir = o.getDirection();

            if ("userRole".equals(prop)) {
                hasUserRoleOrder = true;
                String roleRankCase =
                        "(CASE " +
                                " WHEN u.userRole = 'GUEST'     THEN 0 " +
                                " WHEN u.userRole = 'MEMBER'    THEN 1 " +
                                " WHEN u.userRole = 'CORE'      THEN 2 " +
                                " WHEN u.userRole = 'LEAD'      THEN 3 " +
                                " WHEN u.userRole = 'ORGANIZER' THEN 4 " +
                                " WHEN u.userRole = 'ADMIN'     THEN 5 " +
                                " ELSE -1 END)";
                composed = composed.and(JpaSort.unsafe(dir, roleRankCase));
            } else {
                composed = composed.and(Sort.by(new Sort.Order(dir, prop)));
            }
        }

        if (hasUserRoleOrder) {
            composed = composed.and(Sort.by("name").ascending());
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), composed);
    }

    /* ======================= 수정 ======================= */

    @Transactional
    public void updateRoleAndTeam(CustomUserDetails editor, Long targetUserId, UpdateUserRoleTeamRequest req) {
        User editorUser = getEditor(editor);
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

        UserRole editorRole = editorUser.getUserRole();
        UserRole targetCurrentRole = target.getUserRole();

        UserRole newRole = (req.role() != null ? req.role() : targetCurrentRole);
        TeamType requestedTeam = (req.team() != null ? req.team() : target.getTeam());

        // 팀 보유 가능한 역할만 팀 허용 (CORE, LEAD)
        TeamType newTeam = isTeamAssignableRole(newRole) ? requestedTeam : null;

        // 공통: 에디터는 대상의 현재/신규 role보다 엄격히 높아야 함
        if (!(editorRole.rank() > targetCurrentRole.rank())) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER, "동급/상위 사용자의 정보는 변경할 수 없습니다.");
        }
        if (!(editorRole.rank() > newRole.rank())) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER, "자신보다 크거나 같은 권한으로 변경할 수 없습니다.");
        }

        switch (editorRole) {
            case ADMIN -> {
                if (editorUser.getId().equals(target.getId()) && newRole.rank() < UserRole.ADMIN.rank()) {
                    throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER, "자기 자신을 강등할 수 없습니다.");
                }
            }
            case ORGANIZER -> {
                if (targetCurrentRole == UserRole.ADMIN) {
                    throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER, "ADMIN 사용자는 수정할 수 없습니다.");
                }
            }
            case LEAD -> {
                if (editor.getTeam() == null) {
                    throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER, "LEAD 토큰에 팀 정보가 없습니다.");
                }
                if (!(targetCurrentRole == UserRole.MEMBER || targetCurrentRole == UserRole.CORE)) {
                    throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER, "LEAD는 MEMBER/CORE만 수정할 수 있습니다.");
                }
                if (!(newRole == UserRole.MEMBER || newRole == UserRole.CORE)) {
                    throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER, "LEAD는 MEMBER/CORE로만 변경할 수 있습니다.");
                }

                if (editor.getTeam() == TeamType.HR) {
                    // HR-LEAD: 본인 제외 타인지원 팀 변경 가능
                    if (editorUser.getId().equals(target.getId())) {
                        if (req.team() != null && !Objects.equals(req.team(), target.getTeam())) {
                            throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER, "HR-LEAD도 자기 자신의 팀은 변경할 수 없습니다.");
                        }
                    }
                } else {
                    if (target.getTeam() != editor.getTeam()) {
                        throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER, "다른 팀 사용자는 수정할 수 없습니다.");
                    }
                    if (req.team() != null && !Objects.equals(req.team(), editor.getTeam())) {
                        throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER, "LEAD는 팀을 변경할 수 없습니다.");
                    }
                }
            }
            default -> throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER);
        }

        targetChange(target, newRole, newTeam);
    }

    @Transactional
    public void updateUserRoleWithRules(CustomUserDetails me, Long targetUserId, UserRole newRole) {
        var meRole = me.getRole();
        var meTeam = me.getTeam();

        var target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

        if (Objects.equals(me.getUserId(), targetUserId)) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER, "자기 자신의 역할은 변경할 수 없습니다.");
        }

        UserRole current = target.getUserRole();

        // HR-CORE 특례: GUEST -> MEMBER
        boolean isHrCore = (meRole == UserRole.CORE) && (meTeam == TeamType.HR);
        if (isHrCore) {
            if (current == UserRole.GUEST && newRole == UserRole.MEMBER) {
                target.changeRole(UserRole.MEMBER);
                if (!isTeamAssignableRole(UserRole.MEMBER)) target.changeTeam(null);
                userRepository.save(target);
                return;
            }
            throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER, "HR-CORE는 GUEST→MEMBER 변경만 가능");
        }

        boolean higherThanCurrent = meRole.rank() > current.rank();
        boolean higherThanNew = meRole.rank() > newRole.rank();
        if (!higherThanCurrent || !higherThanNew) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER, "요청한 역할 변경 권한이 없습니다.");
        }

        target.changeRole(newRole);
        if (!isTeamAssignableRole(newRole)) target.changeTeam(null);
        userRepository.save(target);
    }

    @Transactional
    public void deleteUserWithRules(CustomUserDetails me, Long targetUserId) {
        User editor = userRepository.findById(me.getUserId())
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.UNAUTHORIZED_USER));
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.RESOURCE_NOT_FOUND));

        if (Objects.equals(editor.getId(), target.getId())) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER, "자기 자신은 삭제할 수 없습니다.");
        }

        UserRole editorRole = editor.getUserRole();
        TeamType editorTeam = editor.getTeam();
        UserRole targetRole = target.getUserRole();
        TeamType targetTeam = target.getTeam();

        if (!(editorRole.rank() > targetRole.rank())) {
            throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER, "동급/상급 사용자는 삭제할 수 없습니다.");
        }

        switch (editorRole) {
            case ADMIN -> {}
            case ORGANIZER -> {
                if (targetRole == UserRole.ADMIN) {
                    throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER, "ADMIN 사용자는 삭제할 수 없습니다.");
                }
            }
            case LEAD -> {
                if (!(targetRole == UserRole.MEMBER || targetRole == UserRole.CORE)) {
                    throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER, "LEAD는 MEMBER/CORE만 삭제할 수 있습니다.");
                }
                if (editorTeam != TeamType.HR) {
                    if (editorTeam == null || targetTeam != editorTeam) {
                        throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER, "다른 팀 사용자는 삭제할 수 없습니다.");
                    }
                }
            }
            default -> throw new BusinessException(GlobalErrorCode.FORBIDDEN_USER);
        }

        userRepository.delete(target);
    }

    private void targetChange(User target, UserRole newRole, TeamType newTeam) {
        target.changeRole(newRole);
        if (!isTeamAssignableRole(newRole)) newTeam = null;
        target.changeTeam(newTeam);
        userRepository.save(target);
    }

    private User getEditor(CustomUserDetails editor) {
        return userRepository.findById(editor.getUserId())
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.UNAUTHORIZED_USER));
    }

    private boolean isTeamAssignableRole(UserRole role) {
        return role == UserRole.CORE || role == UserRole.LEAD;
    }
}