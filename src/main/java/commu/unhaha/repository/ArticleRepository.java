package commu.unhaha.repository;

import commu.unhaha.domain.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, Long>, ArticleRepositoryCustom {
    Page<Article> findByTitleContaining(String keyword, Pageable pageable);
    Page<Article> findByTitleContainingOrContentContaining(String keyword, String keyword2, Pageable pageable);

    @Modifying
    @Query("UPDATE Article a SET a.viewCount = a.viewCount + 1 WHERE a.id = :articleId")
    void increaseViews(@Param("articleId") Long articleId);
}
