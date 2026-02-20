package inha.gdgoc.domain.auth.repository;

import inha.gdgoc.domain.auth.entity.AdminCredential;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminCredentialRepository extends JpaRepository<AdminCredential, Long> {
    Optional<AdminCredential> findByLoginId(String loginId);
}
