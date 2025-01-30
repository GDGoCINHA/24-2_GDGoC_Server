package inha.gdgoc.domain.user.repository;

import inha.gdgoc.domain.user.entity.User;

import java.util.List;

public interface UserRepositoryCustom {
    List<User> findAllUsers();
}
