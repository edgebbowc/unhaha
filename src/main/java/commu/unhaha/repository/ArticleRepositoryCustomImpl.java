package commu.unhaha.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import commu.unhaha.dto.ArticlesDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static commu.unhaha.domain.QArticle.article;
import static commu.unhaha.domain.QComment.comment;
import static commu.unhaha.domain.QUser.user;


@Repository
@RequiredArgsConstructor
public class ArticleRepositoryCustomImpl implements ArticleRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    // ===== 핵심 통합 메서드 =====
    private Page<ArticlesDto> findArticles(BooleanBuilder condition, OrderSpecifier<?> order, Pageable pageable) {

        List<ArticlesDto> content = queryFactory
                .select(createProjection())
                .from(article)
                .leftJoin(article.user, user)
                .where(condition)
                .orderBy(order)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = queryFactory
                .select(article.count())
                .from(article)
                .leftJoin(article.user, user)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(content, pageable, totalCount != null ? totalCount : 0L);
    }

    /**
     * new Version
     */
    @Override
    public Page<ArticlesDto> findPopularArticles(String searchType, String keyword, Pageable pageable) {
        BooleanBuilder condition = createSearchCondition(searchType, keyword, null)
                .and(article.likeAchievedAt.isNotNull());
        return findArticles(condition, article.likeAchievedAt.desc(), pageable);
    }

    @Override
    public Page<ArticlesDto> findAllArticles(String searchType, String keyword, Pageable pageable) {
        BooleanBuilder condition = createSearchCondition(searchType, keyword, null);
        return findArticles(condition, article.id.desc(), pageable);
    }

    @Override
    public Page<ArticlesDto> findBoardArticles(String boardType, String searchType, String keyword, Pageable pageable) {
        BooleanBuilder condition = createSearchCondition(searchType, keyword, boardType);
        return findArticles(condition, article.id.desc(), pageable);
    }


    // ===== 유틸리티 메서드들 =====
    private ConstructorExpression<ArticlesDto> createProjection() {
        return Projections.constructor(ArticlesDto.class,
                article.id, article.board, article.title, article.content,
                user.nickname, article.viewCount, article.likeCount,
                JPAExpressions.select(comment.count().coalesce(0L))
                        .from(comment).where(comment.article.eq(article)),
                article.createdDate);
    }

    private BooleanBuilder createSearchCondition(String searchType, String keyword, String boardType) {
        BooleanBuilder builder = new BooleanBuilder();

        if (hasText(boardType)) {
            builder.and(article.board.eq(boardType));
        }

        if (hasText(keyword) && hasText(searchType)) {
            builder.and(createSearchExpression(searchType, keyword));
        }

        return builder;
    }

    private BooleanExpression createSearchExpression(String searchType, String keyword) {
        String likeKeyword = "%" + keyword.trim() + "%";
        String type = searchType.toLowerCase();

        if ("title".equals(type)) {
            return article.title.likeIgnoreCase(likeKeyword);
        } else if ("titleandcontent".equals(type)) {
            return article.title.likeIgnoreCase(likeKeyword)
                    .or(article.content.likeIgnoreCase(likeKeyword));
        } else if ("nickname".equals(type)) {
            return user.nickname.likeIgnoreCase(likeKeyword);
        } else {
            // 기본값: 제목 검색
            return article.title.likeIgnoreCase(likeKeyword);
        }
    }

    private boolean hasText(String str) {
        return str != null && !str.trim().isEmpty();
    }
}

