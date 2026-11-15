package inha.gdgoc.domain.homecoming.repository;

import inha.gdgoc.domain.homecoming.entity.HomecomingApplication;
import inha.gdgoc.domain.homecoming.enums.HomecomingStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HomecomingApplicationRepository extends JpaRepository<HomecomingApplication, Long> {

  Optional<HomecomingApplication> findByRequestId(String requestId);

  boolean existsByPhoneAndStatus(String phone, HomecomingStatus status);

  boolean existsByPhoneAndStatusAndIdNot(String phone, HomecomingStatus status, Long id);

  List<HomecomingApplication> findAllByOrderBySubmittedAtAsc();
}
