package inha.gdgoc.domain.recruit.common.repository;

import inha.gdgoc.domain.recruit.common.entity.RecruitPeriodOverride;
import inha.gdgoc.domain.recruit.common.enums.RecruitType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruitPeriodOverrideRepository
    extends JpaRepository<RecruitPeriodOverride, RecruitType> {
}
