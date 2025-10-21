package inha.gdgoc.domain.user.repository;

import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {

    boolean existsByNameAndEmail(String name, String email);
    boolean existsByEmail(String email);

    /* ===== 출석/팀 뷰용 기본 쿼리 ===== */

    // 특정 팀에서 주어진 역할들(CORE/LEAD 등) 만 조회
    List<User> findByTeamAndUserRoleIn(TeamType team, Collection<UserRole> roles);

    // 전체에서 주어진 역할들만 조회(오거나이저/어드민용 전체 뷰)
    List<User> findByUserRoleIn(Collection<UserRole> roles);

    // 리드가 본인 팀 멤버 단일 사용자 검증/조회용
    Optional<User> findByIdAndTeam(Long id, TeamType team);

    // 필요 시: 특정 팀 전체 멤버(역할 무관)
    List<User> findByTeam(TeamType team);
}