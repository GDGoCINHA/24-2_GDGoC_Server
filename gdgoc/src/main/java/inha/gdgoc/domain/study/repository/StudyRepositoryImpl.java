package inha.gdgoc.domain.study.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import inha.gdgoc.domain.study.entity.QStudy;
import inha.gdgoc.domain.study.entity.Study;
import inha.gdgoc.domain.user.entity.QUser;

import java.util.Optional;


public class StudyRepositoryImpl implements StudyRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public StudyRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Optional<Study> findOneWithUserById(Long id) {
        QStudy study = QStudy.study;
        QUser user = QUser.user;
        return Optional.ofNullable(queryFactory
                .selectFrom(study)
                .innerJoin(study.user, user).fetchJoin()
                .fetchOne()
        );
    }
}
