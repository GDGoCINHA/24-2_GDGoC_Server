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

    Optional<RefreshToken> findByUserIdAndToken(Long userId, String token);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.user.id = :user_id AND r.token = :token")
    int deleteByUserIdAndToken(Long user_id, String token);
}
