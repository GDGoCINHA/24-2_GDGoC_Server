package inha.gdgoc.domain.recruit.core.repository;

import inha.gdgoc.domain.recruit.core.entity.RecruitCoreApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecruitCoreApplicationRepository extends JpaRepository<RecruitCoreApplication, Long>,
    JpaSpecificationExecutor<RecruitCoreApplication> {
    Page<RecruitCoreApplication> findByNameContainingIgnoreCase(String name, Pageable pageable);

    java.util.Optional<RecruitCoreApplication> findByUserIdAndSession(Long userId, String session);

    java.util.Optional<RecruitCoreApplication> findByIdAndUserId(Long id, Long userId);
}
