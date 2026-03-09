package inha.gdgoc.domain.game.entity;

import inha.gdgoc.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "game_scores")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Builder
public class Rythm8beatScore extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "phone_number", nullable = false, unique = true, length = 20)
    private String phoneNumber;

    @Column(name = "nickname", nullable = false, length = 20)
    private String nickname;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "stage_reached", nullable = false)
    private int stageReached;

    /**
     * 새 점수가 현재 점수보다 높을 경우 엔티티의 닉네임, 점수 및 도달 스테이지를 갱신한다.
     *
     * @param nickname         갱신할 닉네임
     * @param newScore         비교할 새 점수(현재 점수보다 클 때 적용됨)
     * @param newStageReached  갱신할 도달한 스테이지
     */
    public void updateIfHigherScore(String nickname, int newScore, int newStageReached) {
        if (this.score < newScore) {
            this.nickname = nickname;
            this.score = newScore;
            this.stageReached = newStageReached;
        }
    }
}
