package inha.gdgoc.domain.auth.repository;

import inha.gdgoc.domain.auth.entity.RefreshToken;
import inha.gdgoc.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByUser(User user);

    Optional<RefreshToken> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
