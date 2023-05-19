package commu.unhaha.repository;

import commu.unhaha.domain.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    Page<Article> findByTitleContaining(String keyword, Pageable pageable);
    Page<Article> findByTitleContainingOrContentContaining(String keyword, String keyword2, Pageable pageable);

}
