package commu.unhaha.service;

import commu.unhaha.domain.Article;
import commu.unhaha.domain.UploadFile;
import commu.unhaha.domain.User;
import commu.unhaha.dto.ArticleDto;
import commu.unhaha.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;

    //Article 수정
    public void editArticle(Long articleId, String board, String title, String content) {
        Article article = articleRepository.findById(articleId).orElse(null);
        article.changeArticle(board, title, content);
    }

    public void deleteArticle(Long articleId) {
        articleRepository.deleteById(articleId);
    }

    // 페이징
    public Page<ArticleDto> pageList(int page) {
        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "id"));
        Page<Article> articlePage = articleRepository.findAll(pageable);
        Page<ArticleDto> articleDtoPage = articlePage.map(article -> new ArticleDto(article));
        return articleDtoPage;
    }
}

