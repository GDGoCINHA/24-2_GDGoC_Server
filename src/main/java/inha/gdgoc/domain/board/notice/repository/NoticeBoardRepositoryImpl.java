package inha.gdgoc.domain.board.notice.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import inha.gdgoc.domain.board.common.enums.SearchType;
import inha.gdgoc.domain.board.notice.entity.NoticeBoard;
import inha.gdgoc.domain.board.notice.entity.QNoticeBoard;
import inha.gdgoc.domain.user.enums.UserRole;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NoticeBoardRepositoryImpl implements NoticeBoardRepository {

  private final NoticeBoardJpaRepository jpaRepository;
  private final JPAQueryFactory queryFactory;

  @Override
  public Optional<NoticeBoard> findById(Long id) {
    return jpaRepository.findByIdAndDeletedAtIsNull(id);
  }

  @Override
  public Optional<NoticeBoard> findDeletedById(Long id) {
    return jpaRepository.findByIdAndDeletedAtIsNotNull(id);
  }

  @Override
  public List<NoticeBoard> findAllByIdIn(List<Long> ids) {
    return jpaRepository.findAllByIdInAndDeletedAtIsNull(ids);
  }

  @Override
  public NoticeBoard save(NoticeBoard notice) {
    return jpaRepository.save(notice);
  }

  @Override
  public Page<NoticeBoard> findVisibleNotices(
      UserRole userRole, SearchType searchType, String keyword, Pageable pageable) {

    QNoticeBoard notice = QNoticeBoard.noticeBoard;

    BooleanExpression where = notice.deletedAt.isNull();
    if (!UserRole.hasAtLeast(userRole, UserRole.CORE)) {
      where = where.and(notice.isPublished.isTrue());
    }

    BooleanExpression search = searchCondition(notice, searchType, keyword);
    if (search != null) {
      where = where.and(search);
    }

    List<NoticeBoard> content =
        queryFactory
            .selectFrom(notice)
            .where(where)
            .orderBy(notice.createdAt.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

    Long total = queryFactory.select(notice.count()).from(notice).where(where).fetchOne();

    return new PageImpl<>(content, pageable, total == null ? 0 : total);
  }

  @Override
  public Page<NoticeBoard> findDeletedNotices(Long userId, UserRole userRole, Pageable pageable) {

    QNoticeBoard notice = QNoticeBoard.noticeBoard;

    BooleanExpression where = notice.deletedAt.isNotNull();
    if (!UserRole.hasAtLeast(userRole, UserRole.ORGANIZER)) {
      where = where.and(notice.authorId.eq(userId));
    }

    List<NoticeBoard> content =
        queryFactory
            .selectFrom(notice)
            .where(where)
            .orderBy(notice.deletedAt.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

    Long total = queryFactory.select(notice.count()).from(notice).where(where).fetchOne();

    return new PageImpl<>(content, pageable, total == null ? 0 : total);
  }

  private BooleanExpression searchCondition(
      QNoticeBoard notice, SearchType searchType, String keyword) {
    if (keyword == null || keyword.isBlank()) return null;

    return switch (searchType) {
      case TITLE -> notice.title.containsIgnoreCase(keyword);
      case CONTENT -> notice.content.containsIgnoreCase(keyword);
      case TITLE_AND_CONTENT ->
          notice.title.containsIgnoreCase(keyword).or(notice.content.containsIgnoreCase(keyword));
      case AUTHOR -> notice.authorName.containsIgnoreCase(keyword);
    };
  }
}
