package inha.gdgoc.domain.game.repository;

import inha.gdgoc.domain.game.entity.Rythm8beatScore;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface Rythm8beatScoreRepository extends JpaRepository<Rythm8beatScore, Long> {

    /**
 * 주어진 휴대전화 번호로 저장된 점수 엔티티를 조회합니다.
 *
 * @param phoneNumber 조회할 휴대전화 번호
 * @return `Optional`에 조회된 Rythm8beatScore가 포함되어 있으면 해당 엔티티, 없으면 비어 있음
 */
Optional<Rythm8beatScore> findByPhoneNumber(String phoneNumber);

    /**
 * 상위 3개의 Rythm8beatScore 엔티티를 점수 내림차순으로, 동점일 경우 업데이트 시간 오름차순으로 조회합니다.
 *
 * @return 정렬된 최대 3개의 Rythm8beatScore 목록. 일치하는 엔티티가 없으면 빈 리스트를 반환합니다.
 */
List<Rythm8beatScore> findTop3ByOrderByScoreDescUpdatedAtAsc();

    /**
     * 지정한 점수보다 큰 Rythm8beatScore 엔티티의 개수를 계산합니다.
     *
     * @param score 비교 기준이 되는 점수(이 값을 초과하는 항목을 셀 임계값)
     * @return 지정한 `score`보다 큰 점수를 가진 엔티티의 수
     */
    @Query("SELECT COUNT(r) FROM Rythm8beatScore r WHERE r.score > :score")
    long countByScoreGreaterThan(@Param("score") int score);
}
