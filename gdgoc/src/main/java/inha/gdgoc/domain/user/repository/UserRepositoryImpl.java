package inha.gdgoc.domain.user.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import inha.gdgoc.domain.user.entity.QUser;
import inha.gdgoc.domain.user.entity.User;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public UserRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<User> findAllUsers() {
        QUser user = QUser.user;
        return queryFactory
                .selectFrom(user)
                .fetch();
    }
}
