package inha.service;

import inha.gdgoc.GdgocApplication;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.CoreType;
import inha.gdgoc.domain.user.enums.Interest;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.domain.user.service.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest(classes = GdgocApplication.class)
@Transactional
public class UserServiceTest {

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Test
    public void queryTest() {
        // given
        User user1 = User.create(
                "1",
                CoreType.HR_CORE,
                "1", "1", "1", "1", Interest.AI, "1", "1"
        );

        User user2 = User.create(
                "1",
                CoreType.HR_CORE,
                "1", "1", "1", "1", Interest.AI, "1", "1"
        );

        userRepository.saveAll(List.of(user1, user2));

        List<Long> findUserIds = List.of(user1.getId(), user2.getId());

        // when
        List<Long> resultIds = userService.getAllUserIds();

        // then
        findUserIds.containsAll(resultIds);
    }
}
