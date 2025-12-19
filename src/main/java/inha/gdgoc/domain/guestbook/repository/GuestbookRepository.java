package inha.gdgoc.domain.guestbook.repository;

import inha.gdgoc.domain.guestbook.entity.GuestbookEntry;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GuestbookRepository extends JpaRepository<GuestbookEntry, Long> {

    boolean existsByWristbandSerial(String wristbandSerial);

    // 목록
    List<GuestbookEntry> findAllByOrderByCreatedAtDesc();

    // 당첨 전 후보(락)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GuestbookEntry g where g.wonAt is null")
    List<GuestbookEntry> findAllByWonAtIsNullForUpdate();

    // 당첨자 목록
    List<GuestbookEntry> findAllByWonAtIsNotNullOrderByWonAtAsc();

    // 리셋 (wonAt = null)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update GuestbookEntry g set g.wonAt = null where g.wonAt is not null")
    long clearAllWinners();
}
