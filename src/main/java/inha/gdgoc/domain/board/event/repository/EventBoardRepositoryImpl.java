package inha.gdgoc.domain.board.event.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import inha.gdgoc.domain.board.event.entity.EventBoard;
import inha.gdgoc.domain.board.event.entity.QEventBoard;
import inha.gdgoc.domain.board.common.enums.SearchType;
import inha.gdgoc.domain.user.enums.TeamType;
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
public class EventBoardRepositoryImpl implements EventBoardRepository {

  private final EventBoardJpaRepository jpaRepository;
  private final JPAQueryFactory queryFactory;

  @Override
  public Optional<EventBoard> findById(Long id) {
    return jpaRepository.findByIdAndDeletedAtIsNull(id);
  }

  @Override
  public Optional<EventBoard> findDeletedById(Long id) {
    return jpaRepository.findByIdAndDeletedAtIsNotNull(id);
  }

  @Override
  public EventBoard save(EventBoard board) {
    return jpaRepository.save(board);
  }

  @Override
  public void delete(EventBoard board) {
    jpaRepository.delete(board);
  }

  @Override
  public Page<EventBoard> findVisibleBoards(
      TeamType userTeam, UserRole userRole, SearchType searchType, String keyword, Pageable pageable) {

    QEventBoard board = QEventBoard.eventBoard;

    BooleanExpression visibility = visibilityCondition(board, userTeam, userRole);
    BooleanExpression search = searchCondition(board, searchType, keyword);

    BooleanExpression where = visibility;
    if (search != null) {
      where = where.and(search);
    }

    List<EventBoard> content =
        queryFactory
            .selectFrom(board)
            .where(where)
            .orderBy(board.createdAt.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

    Long total =
        queryFactory.select(board.count()).from(board).where(where).fetchOne();

    return new PageImpl<>(content, pageable, total == null ? 0 : total);
  }

  @Override
  public Page<EventBoard> findDeletedBoards(TeamType userTeam, UserRole userRole, Pageable pageable) {
    QEventBoard board = QEventBoard.eventBoard;

    BooleanExpression where = board.deletedAt.isNotNull();

    if (!UserRole.hasAtLeast(userRole, UserRole.ORGANIZER)) {
      if (userTeam == null) return Page.empty(pageable);
      where = where.and(board.organizingTeam.eq(userTeam));
    }

    List<EventBoard> content =
        queryFactory
            .selectFrom(board)
            .where(where)
            .orderBy(board.deletedAt.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

    Long total =
        queryFactory.select(board.count()).from(board).where(where).fetchOne();

    return new PageImpl<>(content, pageable, total == null ? 0 : total);
  }

  private BooleanExpression visibilityCondition(QEventBoard board, TeamType userTeam, UserRole userRole) {
    BooleanExpression notDeleted = board.deletedAt.isNull();

    // ORGANIZER+: 모든 활성 게시글 조회 가능
    if (userRole != null && UserRole.hasAtLeast(userRole, UserRole.ORGANIZER)) {
      return notDeleted;
    }
    // 팀 없음 (비로그인·MEMBER): 공개 게시글만
    if (userTeam == null) {
      return notDeleted.and(board.isPublished.isTrue());
    }
    // CORE·LEAD: 공개 + 같은 팀 비공개
    return notDeleted.and(
        board.isPublished.isTrue()
            .or(board.isPublished.isFalse().and(board.organizingTeam.eq(userTeam))));
  }

  private BooleanExpression searchCondition(
      QEventBoard board, SearchType searchType, String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return null;
    }
    return switch (searchType) {
      case TITLE -> board.title.containsIgnoreCase(keyword);
      case CONTENT -> board.content.containsIgnoreCase(keyword);
      case TITLE_AND_CONTENT ->
          board.title.containsIgnoreCase(keyword).or(board.content.containsIgnoreCase(keyword));
      case AUTHOR -> board.authorName.containsIgnoreCase(keyword);
    };
  }
}
