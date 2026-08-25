package inha.gdgoc.domain.landing.repository;

import inha.gdgoc.domain.landing.entity.LandingContent;
import inha.gdgoc.domain.landing.enums.LandingContentStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LandingContentRepository extends JpaRepository<LandingContent, Long> {

    Optional<LandingContent> findByStatus(LandingContentStatus status);
}
