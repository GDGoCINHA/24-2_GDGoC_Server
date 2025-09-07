package inha.gdgoc.domain.core.recruit.repository;

import inha.gdgoc.domain.core.recruit.entity.CoreRecruitApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoreRecruitApplicationRepository extends JpaRepository<CoreRecruitApplication, Long> {
    Page<CoreRecruitApplication> findByNameContainingIgnoreCase(String name, Pageable pageable);
}


