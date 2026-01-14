package inha.gdgoc.domain.recruit.member.repository;

import inha.gdgoc.domain.recruit.member.entity.RecruitMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RecruitMemberRepository extends JpaRepository<RecruitMember, Long> {
    boolean existsByStudentId(String studentId);
    boolean existsByPhoneNumber(String phoneNumber);
    Page<RecruitMember> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
