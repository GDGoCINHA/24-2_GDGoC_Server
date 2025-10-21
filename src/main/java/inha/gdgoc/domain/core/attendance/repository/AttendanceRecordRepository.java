package inha.gdgoc.domain.core.attendance.repository;

import inha.gdgoc.domain.core.attendance.entity.AttendanceRecord;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    /* 조회: 특정 날짜의 (userId, present) 목록 */
    @Query("""
        select ar.user.id, ar.present
        from AttendanceRecord ar
        where ar.meetingDate = :date
    """)
    List<Object[]> findPresencePairsByDate(@Param("date") LocalDate date);

    /* 삭제: 해당 날짜의 출석 레코드 전체 삭제 */
    void deleteByMeetingDate(LocalDate date);

    /* 배치 업서트(ON CONFLICT) — 고성능로 present를 일괄 반영 */
    @Modifying
    @Query(value = """
        INSERT INTO public.attendance_records (meeting_date, user_id, present, updated_at)
        SELECT :date, u, :present, NOW()
        FROM unnest(:userIds) AS u
        ON CONFLICT (meeting_date, user_id)
        DO UPDATE SET present = EXCLUDED.present, updated_at = NOW()
    """, nativeQuery = true)
    int upsertBatch(@Param("date") LocalDate date,
                    @Param("userIds") List<Long> userIds,
                    @Param("present") boolean present);
}