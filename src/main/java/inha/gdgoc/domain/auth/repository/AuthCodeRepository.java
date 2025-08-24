package inha.gdgoc.domain.auth.repository;

import inha.gdgoc.domain.auth.entity.AuthCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthCodeRepository extends JpaRepository<AuthCode, Long> {
    Optional<AuthCode> findByEmail(String email);
    void deleteByEmail(String email);

    boolean existsByEmail(String email);
}

