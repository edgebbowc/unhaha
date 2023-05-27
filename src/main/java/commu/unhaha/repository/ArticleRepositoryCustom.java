package commu.unhaha.repository;

import commu.unhaha.domain.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ArticleRepositoryCustom {
    Page<Article> findByNicknamePaging(String keyword, Pageable pageable);
}
