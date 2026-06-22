package inha.gdgoc.domain.board.notice.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import inha.gdgoc.domain.board.notice.dto.request.NoticeSearchCondition;
import inha.gdgoc.domain.board.notice.entity.NoticeBoard;
import inha.gdgoc.domain.board.notice.entity.QNoticeBoard;
import inha.gdgoc.domain.board.notice.enums.ArticleStatusEnum;
import inha.gdgoc.domain.board.notice.enums.CategoryEnum;
import inha.gdgoc.domain.board.notice.enums.SearchTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NoticeBoardRepositoryImpl implements NoticeBoardRepository {

    private final NoticeBoardJpaRepository jpaRepository;
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<NoticeBoard> findById(UUID id) {
        return jpaRepository.findByArticleIdAndDeletedAtIsNull(id);
    }

    @Override
    public Optional<NoticeBoard> findDeletedById(UUID id) {
        return jpaRepository.findByArticleIdAndDeletedAtIsNotNull(id);
    }

    @Override
    public NoticeBoard save(NoticeBoard board) {
        return jpaRepository.save(board);
    }

    @Override
    public void delete(NoticeBoard board) {
        jpaRepository.delete(board);
    }

    @Override
    public void clearAllPinned() {
        QNoticeBoard board = QNoticeBoard.noticeBoard;
        queryFactory.update(board)
                .set(board.isPinned, false)
                .where(board.isPinned.isTrue())
                .execute();
    }

    @Override
    public Page<NoticeBoard> findVisibleBoards(NoticeSearchCondition condition, Pageable pageable) {
        QNoticeBoard board = QNoticeBoard.noticeBoard;

        BooleanExpression where = board.status.eq(ArticleStatusEnum.PUBLISHED)
                .and(board.deletedAt.isNull());

        if (condition.category() != null) {
            where = where.and(board.category.eq(condition.category()));
        }

        BooleanExpression search = searchCondition(board, condition.searchType(), condition.keyword());
        if (search != null) {
            where = where.and(search);
        }

        List<NoticeBoard> content = queryFactory.selectFrom(board)
                .where(where)
                .orderBy(board.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory.select(board.count())
                .from(board)
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    @Override
    public List<NoticeBoard> findPinnedNotices() {
        QNoticeBoard board = QNoticeBoard.noticeBoard;
        return queryFactory.selectFrom(board)
                .where(board.isPinned.isTrue()
                        .and(board.status.eq(ArticleStatusEnum.PUBLISHED))
                        .and(board.deletedAt.isNull()))
                .orderBy(board.createdAt.desc())
                .fetch();
    }

    @Override
    public Page<NoticeBoard> findDeletedBoards(NoticeSearchCondition condition, Pageable pageable) {
        QNoticeBoard board = QNoticeBoard.noticeBoard;

        BooleanExpression where = board.status.eq(ArticleStatusEnum.DELETED)
                .and(board.deletedAt.isNotNull());

        if (condition.category() != null) {
            where = where.and(board.category.eq(condition.category()));
        }

        BooleanExpression search = searchCondition(board, condition.searchType(), condition.keyword());
        if (search != null) {
            where = where.and(search);
        }

        List<NoticeBoard> content = queryFactory.selectFrom(board)
                .where(where)
                .orderBy(board.deletedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory.select(board.count())
                .from(board)
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    @Override
    public Optional<NoticeBoard> findPrevNotice(Long currentArticleNumber, CategoryEnum category) {
        QNoticeBoard board = QNoticeBoard.noticeBoard;

        BooleanExpression where = board.articleNumber.lt(currentArticleNumber)
                .and(board.status.eq(ArticleStatusEnum.PUBLISHED))
                .and(board.deletedAt.isNull())
                .and(categoryEq(board, category));

        NoticeBoard result = queryFactory.selectFrom(board)
                .where(where)
                .orderBy(board.articleNumber.desc())
                .fetchFirst();

        return Optional.ofNullable(result);
    }

    @Override
    public Optional<NoticeBoard> findNextNotice(Long currentArticleNumber, CategoryEnum category) {
        QNoticeBoard board = QNoticeBoard.noticeBoard;

        BooleanExpression where = board.articleNumber.gt(currentArticleNumber)
                .and(board.status.eq(ArticleStatusEnum.PUBLISHED))
                .and(board.deletedAt.isNull())
                .and(categoryEq(board, category));

        NoticeBoard result = queryFactory.selectFrom(board)
                .where(where)
                .orderBy(board.articleNumber.asc())
                .fetchFirst();

        return Optional.ofNullable(result);
    }

    private BooleanExpression categoryEq(QNoticeBoard board, CategoryEnum category) {
        if (category == null) {
            return board.category.isNull();
        }
        return board.category.eq(category);
    }

    private BooleanExpression searchCondition(QNoticeBoard board, SearchTypeEnum searchType, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        if (searchType == null) {
            searchType = SearchTypeEnum.TITLE_CONTENT;
        }
        return switch (searchType) {
            case TITLE -> board.title.containsIgnoreCase(keyword);
            case CONTENT -> board.content.containsIgnoreCase(keyword);
            case TITLE_CONTENT -> board.title.containsIgnoreCase(keyword)
                    .or(board.content.containsIgnoreCase(keyword));
            case AUTHOR -> board.postedByName.containsIgnoreCase(keyword);
        };
    }
}
