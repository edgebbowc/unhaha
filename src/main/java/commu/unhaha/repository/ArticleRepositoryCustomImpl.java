package commu.unhaha.repository;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import commu.unhaha.domain.Article;
import commu.unhaha.domain.QUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static commu.unhaha.domain.QArticle.article;
import static commu.unhaha.domain.QUser.user;


@Repository
@RequiredArgsConstructor
public class ArticleRepositoryCustomImpl implements ArticleRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Article> findByNicknamePaging(String keyword, Pageable pageable) {
        List<Article> content = queryFactory.select(article)
                .from(article)
                .join(article.user, user).fetchJoin()
                .where(user.nickname.eq(keyword))
                .orderBy(article.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory.select(article.count())
                .from(article)
                .join(article.user, user)
                .where(user.nickname.eq(keyword));



        return PageableExecutionUtils.getPage(content, pageable, () -> countQuery.fetchOne()); //카운트 쿼리 최적화
//        return new PageImpl<>(content, pageable, total);
    }
}
