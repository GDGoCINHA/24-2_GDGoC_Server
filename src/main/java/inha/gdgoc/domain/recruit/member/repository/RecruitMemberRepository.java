package inha.gdgoc.domain.recruit.member.repository;

import inha.gdgoc.domain.recruit.member.entity.RecruitMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecruitMemberRepository
        extends JpaRepository<RecruitMember, Long>, JpaSpecificationExecutor<RecruitMember> {
    boolean existsByStudentId(String studentId);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByEmailIgnoreCase(String email);
}
