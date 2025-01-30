package inha.gdgoc.domain.recruit.repository;

import inha.gdgoc.domain.recruit.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
