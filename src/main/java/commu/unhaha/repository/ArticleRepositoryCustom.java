package commu.unhaha.repository;

import commu.unhaha.domain.Article;
import commu.unhaha.dto.ArticlesDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ArticleRepositoryCustom {
    Page<ArticlesDto> findPopularArticles(String searchType, String keyword, Pageable pageable);

    Page<ArticlesDto> findAllArticles(String searchType, String keyword, Pageable pageable);

    Page<ArticlesDto> findBoardArticles(String boardType, String searchType, String keyword, Pageable pageable);
}
