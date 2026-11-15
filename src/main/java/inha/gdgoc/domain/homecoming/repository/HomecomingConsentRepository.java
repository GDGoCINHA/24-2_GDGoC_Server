package inha.gdgoc.domain.homecoming.repository;

import inha.gdgoc.domain.homecoming.entity.HomecomingConsent;
import inha.gdgoc.domain.homecoming.enums.ConsentPurpose;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HomecomingConsentRepository extends JpaRepository<HomecomingConsent, Long> {

  List<HomecomingConsent> findAllByApplicationIdIn(Collection<Long> applicationIds);

  List<HomecomingConsent> findAllByApplicationId(Long applicationId);

  Optional<HomecomingConsent> findByApplicationIdAndPurpose(Long applicationId, ConsentPurpose purpose);

  @EntityGraph(attributePaths = "application")
  List<HomecomingConsent> findAllByPurposeOrderByDecidedAtAsc(ConsentPurpose purpose);
}
