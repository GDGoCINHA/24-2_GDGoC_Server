package inha.gdgoc.domain.manito.repository;

import inha.gdgoc.domain.manito.entity.ManitoSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ManitoSessionRepository extends JpaRepository<ManitoSession, Long> {

    /**
     * 세션 코드로 조회 (예: "gmg-2025")
     */
    Optional<ManitoSession> findByCode(String code);

    /**
     * 세션 코드 중복 체크용
     */
    boolean existsByCode(String code);
}