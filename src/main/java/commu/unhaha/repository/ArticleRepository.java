package commu.unhaha.repository;

import commu.unhaha.domain.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long>, ArticleRepositoryCustom {

    // 전체글 이전글/다음글 (기존과 동일)
    Article findTopByIdLessThanOrderByIdDesc(Long id);
    Article findTopByIdGreaterThanOrderByIdAsc(Long id);

    // 인기글 이전글/다음글 (likeAchievedAt 기준으로 변경)
    Article findTopByLikeAchievedAtLessThanOrderByLikeAchievedAtDesc(LocalDateTime likeAchievedAt);
    Article findTopByLikeAchievedAtGreaterThanOrderByLikeAchievedAtAsc(LocalDateTime likeAchievedAt);

    // 보디빌딩 게시글 이전글/다음글
    Article findTopByIdLessThanAndBoardOrderByIdDesc(Long id, String board);
    Article findTopByIdGreaterThanAndBoardOrderByIdAsc(Long id, String board);

    @Modifying
    @Query("UPDATE Article a SET a.viewCount = a.viewCount + 1 WHERE a.id = :articleId")
    void increaseViews(@Param("articleId") Long articleId);

    @Query("SELECT a FROM Article a " +
            "JOIN FETCH a.user " +
            "WHERE a.user.email = :email " +
            "ORDER BY a.createdDate DESC")
    List<Article> findByUserEmailOrderByCreatedDateDesc(@Param("email") String email);

    @Query("SELECT a FROM Article a " +
            "JOIN FETCH a.user " +
            "JOIN UserLikeArticle ula ON a.id = ula.article.id " +
            "JOIN User u ON ula.user.id = u.id " +
            "WHERE u.email = :email " +
            "ORDER BY ula.createdDate DESC")
    List<Article> findLikedArticlesByUserEmail(@Param("email") String email);
}
