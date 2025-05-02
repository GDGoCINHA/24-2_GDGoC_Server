package inha.gdgoc.domain.user.service;

import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<Long> getAllUserIds() {
        return userRepository.findAllUsers().stream()
                .map((User::getId))
                .toList();
    }

    public User findUserById(Long userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("User not found user id: " + userId));
    }
}
