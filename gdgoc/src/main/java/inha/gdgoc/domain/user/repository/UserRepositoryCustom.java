package inha.gdgoc.domain.user.repository;

import inha.gdgoc.domain.user.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryCustom {
    List<User> findAllUsers();

    Optional<User> findByUserId(Long userId);
    Optional<User> findByEmail(String email);
    Optional<User> findByNameAndMajorAndPhoneNumber(String name, String major, String phoneNumber);
}
