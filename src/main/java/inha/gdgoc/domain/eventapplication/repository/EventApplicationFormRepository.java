package inha.gdgoc.domain.eventapplication.repository;

import inha.gdgoc.domain.eventapplication.entity.EventApplicationForm;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventApplicationFormRepository extends JpaRepository<EventApplicationForm, Long> {

  Optional<EventApplicationForm> findByEventBoardId(Long eventBoardId);

  boolean existsByEventBoardId(Long eventBoardId);

  /**
   * 정원을 세기 전에 폼 행을 잠근다.
   *
   * <p>잠그지 않으면 신청이 몰릴 때 여러 요청이 {@code count < capacity} 를 동시에 통과해 정원을 넘긴다. 우리 규모에서는 이 한 줄이면 충분하고,
   * 카운터 컬럼이나 낙관적 락을 따로 둘 이유가 없다.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select f from EventApplicationForm f where f.eventBoardId = :eventBoardId")
  Optional<EventApplicationForm> findByEventBoardIdForUpdate(@Param("eventBoardId") Long eventBoardId);
}
