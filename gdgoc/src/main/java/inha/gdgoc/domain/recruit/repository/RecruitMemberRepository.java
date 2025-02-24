package inha.gdgoc.domain.recruit.repository;

import inha.gdgoc.domain.recruit.entity.RecruitMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruitMemberRepository extends JpaRepository<RecruitMember, Long> {
    boolean existsByStudentId(String studentId);
    boolean existsByPhoneNumber(String phoneNumber);
}
