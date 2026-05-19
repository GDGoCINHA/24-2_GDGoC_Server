package inha.gdgoc.domain.board.event.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import inha.gdgoc.domain.board.event.entity.EventBoard;
import inha.gdgoc.domain.board.event.entity.QEventBoard;
import inha.gdgoc.domain.board.event.enums.SearchType;
import inha.gdgoc.domain.user.enums.TeamType;
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
    return jpaRepository.findById(id);
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
      TeamType userTeam, SearchType searchType, String keyword, Pageable pageable) {

    QEventBoard board = QEventBoard.eventBoard;

    BooleanExpression visibility = visibilityCondition(board, userTeam);
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

  private BooleanExpression visibilityCondition(QEventBoard board, TeamType userTeam) {
    BooleanExpression publicPosts = board.isPublished.isTrue();
    if (userTeam == null) {
      return publicPosts;
    }
    return publicPosts.or(
        board.isPublished.isFalse().and(board.organizingTeam.eq(userTeam)));
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
    };
  }
}
