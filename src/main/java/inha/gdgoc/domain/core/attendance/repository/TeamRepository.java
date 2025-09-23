// inha/gdgoc/domain/core/attendance/repository/TeamRepository.java
package inha.gdgoc.domain.core.attendance.repository;

import inha.gdgoc.domain.core.attendance.entity.Team;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface TeamRepository {
    Team save(Team team);
    Optional<Team> findById(String id);
    Collection<Team> findAll();
    void deleteById(String id);
}