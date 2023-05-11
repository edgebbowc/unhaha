package commu.unhaha.service;

import commu.unhaha.domain.Article;
import commu.unhaha.dto.ArticleDto;
import commu.unhaha.dto.ArticlesDto;
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
    private final RedisService redisService;

    //Article 수정
    public void editArticle(Long articleId, String board, String title, String content) {
        Article article = articleRepository.findById(articleId).orElse(null);
        article.changeArticle(board, title, content);
    }

    public void deleteArticle(Long articleId) {
        articleRepository.deleteById(articleId);
    }

    // 페이징
    public Page<ArticlesDto> pageList(int page) {
        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "id"));
        Page<Article> articlePage = articleRepository.findAll(pageable);
        Page<ArticlesDto> articleDtoPage = articlePage.map(article -> new ArticlesDto(article));
        return articleDtoPage;
    }

    public void addViewCount(Article article, String clientAddress) {
        article.increaseViewCount();
        redisService.writeClientRequest(clientAddress, article.getId());
    }

    public ArticleDto NoneMemberView(Long articleId, String clientAddress) {
        Article article = articleRepository.findById(articleId).orElse(null);
        if (redisService.isFirstIpRequest(clientAddress, articleId)) {
            addViewCount(article, clientAddress);
        }
        ArticleDto articleDto = new ArticleDto(article);
        return articleDto;
    }

    public ArticleDto MemberView(Long articleId, String email) {
        Article article = articleRepository.findById(articleId).orElse(null);
        String key = email;
        String value = articleId.toString();
        if (!redisService.getValuesList(key).contains(value)) {
            redisService.setValuesList(key, value);
            article.increaseViewCount();
        }
        ArticleDto articleDto = new ArticleDto(article);
        return articleDto;
    }
}

