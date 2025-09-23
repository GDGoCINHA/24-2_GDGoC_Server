// inha/gdgoc/domain/core/attendance/repository/MemberRepository.java
package inha.gdgoc.domain.core.attendance.repository;

import inha.gdgoc.domain.core.attendance.entity.Member;
import inha.gdgoc.domain.core.attendance.entity.Team;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository {
    Member add(Team team, Member member);
    Optional<Member> find(Team team, String memberId);
    void remove(Team team, String memberId);
}