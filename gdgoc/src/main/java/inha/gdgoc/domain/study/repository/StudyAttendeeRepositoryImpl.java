package inha.gdgoc.domain.study.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import inha.gdgoc.domain.study.entity.QStudy;
import inha.gdgoc.domain.study.entity.QStudyAttendee;
import inha.gdgoc.domain.study.entity.StudyAttendee;
import inha.gdgoc.domain.user.entity.QUser;

import java.util.List;

public class StudyAttendeeRepositoryImpl implements StudyAttendeeCustom {

    private final JPAQueryFactory queryFactory;

    public StudyAttendeeRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<StudyAttendee> pageAllByStudyId(Long studyId, Long limit, Long offset) {
        QStudyAttendee studyAttendee = QStudyAttendee.studyAttendee;
        QUser user = QUser.user;
        return queryFactory
                .selectFrom(studyAttendee)
                .innerJoin(studyAttendee.user, user).fetchJoin()
                .where(studyAttendee.study.id.eq(studyId))
                .offset(offset)
                .limit(limit)
                .orderBy(studyAttendee.id.desc())
                .fetch();
    }

    @Override
    public List<StudyAttendee> findAllByIdsAndStudyId(List<Long> ids, Long studyId) {
        QStudyAttendee studyAttendee = QStudyAttendee.studyAttendee;
        return queryFactory
                .selectFrom(studyAttendee)
                .where(studyAttendee.id.in(ids).and(studyAttendee.study.id.eq(studyId)))
                .fetch();
    }

    @Override
    public List<StudyAttendee> findAllByUserId(Long userId) {
        QUser user = QUser.user;
        QStudy study = QStudy.study;
        QStudyAttendee studyAttendee = QStudyAttendee.studyAttendee;
        return queryFactory.selectFrom(studyAttendee)
                .innerJoin(studyAttendee.study, study).fetchJoin()
                .innerJoin(studyAttendee.user, user).fetchJoin()
                .where(user.id.eq(userId))
                .orderBy(studyAttendee.id.desc())
                .fetch();
    }

    @Override
    public Long findAllByStudyIdStudyAttendeeCount(Long studyId) {
        QStudyAttendee studyAttendee = QStudyAttendee.studyAttendee;
        return queryFactory
                .select(studyAttendee.count())
                .where(studyAttendee.study.id.eq(studyId))
                .from(studyAttendee)
                .fetchOne();

    }
}
