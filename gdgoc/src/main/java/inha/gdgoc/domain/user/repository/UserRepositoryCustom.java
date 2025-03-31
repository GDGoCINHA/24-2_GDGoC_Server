package inha.gdgoc.domain.user.repository;

import inha.gdgoc.domain.user.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryCustom {
    List<User> findAllUsers();
    Optional<User> findByEmail(String email);
}
