package inha.gdgoc.domain.board.free.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import inha.gdgoc.domain.board.common.enums.SearchType;
import inha.gdgoc.domain.board.free.entity.FreeBoard;
import inha.gdgoc.domain.board.free.entity.QFreeBoard;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FreeBoardRepositoryImpl implements FreeBoardRepository {

  private final FreeBoardJpaRepository jpaRepository;
  private final JPAQueryFactory queryFactory;

  @Override
  public Optional<FreeBoard> findById(Long id) {
    return jpaRepository.findByIdAndDeletedAtIsNull(id);
  }

  @Override
  public FreeBoard save(FreeBoard post) {
    return jpaRepository.save(post);
  }

  @Override
  public void increaseViewCount(Long id) {
    jpaRepository.increaseViewCount(id);
  }

  @Override
  public Page<FreeBoard> findVisiblePosts(
      SearchType searchType, String keyword, Pageable pageable) {

    QFreeBoard post = QFreeBoard.freeBoard;

    BooleanExpression where = post.deletedAt.isNull();

    BooleanExpression search = searchCondition(post, searchType, keyword);
    if (search != null) {
      where = where.and(search);
    }

    List<FreeBoard> content =
        queryFactory
            .selectFrom(post)
            .where(where)
            .orderBy(post.createdAt.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

    Long total = queryFactory.select(post.count()).from(post).where(where).fetchOne();

    return new PageImpl<>(content, pageable, total == null ? 0 : total);
  }

  private BooleanExpression searchCondition(
      QFreeBoard post, SearchType searchType, String keyword) {
    if (keyword == null || keyword.isBlank()) return null;

    return switch (searchType) {
      case TITLE -> post.title.containsIgnoreCase(keyword);
      case CONTENT -> post.content.containsIgnoreCase(keyword);
      case TITLE_AND_CONTENT ->
          post.title.containsIgnoreCase(keyword).or(post.content.containsIgnoreCase(keyword));
      case AUTHOR -> post.authorName.containsIgnoreCase(keyword);
    };
  }
}
