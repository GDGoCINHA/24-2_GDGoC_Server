package inha.gdgoc.domain.auth.repository;

import inha.gdgoc.domain.auth.entity.RefreshToken;
import inha.gdgoc.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByUser(User user);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.user.id = :userId AND r.token = :token")
    void deleteByUserIdAndToken(Long user_id, String token); // 로그아웃할 때 이 토큰만 삭제
}
