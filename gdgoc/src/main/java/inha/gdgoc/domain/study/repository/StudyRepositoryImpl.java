package inha.gdgoc.domain.study.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import inha.gdgoc.domain.study.entity.QStudy;
import inha.gdgoc.domain.study.entity.Study;
import inha.gdgoc.domain.study.enums.CreaterType;
import inha.gdgoc.domain.study.enums.StudyStatus;
import inha.gdgoc.domain.user.entity.QUser;

import java.util.List;
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

    @Override
    public List<Study> findAllByUserId(Long userId) {
        QStudy study = QStudy.study;
        return queryFactory
                .selectFrom(study)
                .where(study.user.id.eq(userId))
                .orderBy(study.user.id.desc())
                .fetch();
    }

    @Override
    public List<Study> findAllByStatusAndCreatorType(
            Optional<StudyStatus> status, Optional<CreaterType> creatorType,
            Long limit, Long offset) {

        QStudy study = QStudy.study;

        BooleanBuilder builder = new BooleanBuilder();

        status.ifPresent(s -> builder.and(study.status.eq(s)));
        creatorType.ifPresent(c -> builder.and(study.createrType.eq(c)));

        return queryFactory
                .selectFrom(study)
                .where(builder)
                .offset(offset)
                .limit(limit)
                .orderBy(study.id.desc())
                .fetch();
    }

    @Override
    public Long findAllCountByStatusAndCreatorType(Optional<StudyStatus> status, Optional<CreaterType> creatorType) {
        QStudy study = QStudy.study;

        BooleanBuilder builder = new BooleanBuilder();

        status.ifPresent(s -> builder.and(study.status.eq(s)));
        creatorType.ifPresent(c -> builder.and(study.createrType.eq(c)));

        return queryFactory
                .select(study.count())
                .from(study)
                .where(builder)
                .fetchOne();
    }
}
