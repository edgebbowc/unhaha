package commu.unhaha.service;

import commu.unhaha.controller.SessionConst;
import commu.unhaha.domain.Article;
import commu.unhaha.domain.User;
import commu.unhaha.domain.UserLikeArticle;
import commu.unhaha.dto.ArticleDto;
import commu.unhaha.dto.ArticlesDto;
import commu.unhaha.dto.SessionUser;
import commu.unhaha.repository.ArticleRepository;
import commu.unhaha.repository.UserLikeArticleRepository;
import commu.unhaha.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;

@Service
@Transactional
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final RedisService redisService;
    private final UserLikeArticleRepository userLikeArticleRepository;
    private final UserRepository userRepository;

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

    public Page<ArticlesDto> searchPageList(int page, String keyword, String searchType) {
        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "id"));
        switch (searchType) {
            case "title" :
                Page<Article> articlePageT = articleRepository.findByTitleContaining(keyword, pageable);
                Page<ArticlesDto> articleDtoPageT = articlePageT.map(article -> new ArticlesDto(article));
                return articleDtoPageT;
            case "titleAndContent" :
                Page<Article> articlePageTC = articleRepository.findByTitleContainingOrContentContaining(keyword, keyword, pageable);
                Page<ArticlesDto> articleDtoPageTC = articlePageTC.map(article -> new ArticlesDto(article));
                return articleDtoPageTC;
        }

        return null;
    }

    public void addViewCount(Article article, String clientAddress) {
        article.increaseViewCount();
        redisService.writeClientRequest(clientAddress, article.getId());
    }

    public ArticleDto noneMemberView(Long articleId, String clientAddress) {
        Article article = articleRepository.findById(articleId).orElse(null);
        if (redisService.isFirstIpRequest(clientAddress, articleId)) {
            addViewCount(article, clientAddress);
        }
        ArticleDto articleDto = new ArticleDto(article);
        return articleDto;
    }

    public ArticleDto memberView(Long articleId, String email) {
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

    public String calDateTime(LocalDateTime createdDate, LocalDateTime now) {
        Period diff = Period.between(createdDate.toLocalDate(), now.toLocalDate());
        Duration timeDiff = Duration.between(createdDate.toLocalTime(), now.toLocalTime());
        String datetime;

        if (diff.isZero()) {
            if (timeDiff.getSeconds() < 60) {
                String seconds = Long.toString(timeDiff.getSeconds());
                datetime = seconds.concat("초전");
                return datetime;
            } else if (timeDiff.toMinutes() < 60) {
                String minutes = Long.toString(timeDiff.toMinutes());
                datetime = minutes.concat("분전");
                return datetime;
            } else {
                String hours = Long.toString(timeDiff.toHours());
                datetime = hours.concat("시간전");
                return datetime;
            }
        }

        // 1일~7일 -> 1일전
        if (diff.getYears() == 0 && diff.getMonths() == 0 && (0 < diff.getDays() && diff.getDays() <= 7)) {
            String days = Integer.toString(diff.getDays());
            datetime = days.concat("일전");
            return datetime;
        }

        //7일이상 지나면 -> 날짜 출력 ex) 05.20
        datetime = createdDate.format(DateTimeFormatter.ofPattern("MM.dd"));
        return datetime;
    }

    // 게시글 좋아요 확인
    public boolean findLike(Long article_id, Long user_id) {

        return userLikeArticleRepository.existsByArticleIdAndUserId(article_id, user_id);

    }

    public boolean saveLike(Long articleId, Long userId) {

        /** 로그인한 유저가 해당 게시물을 좋아요 했는지 안 했는지 확인 **/
        if(!findLike(articleId, userId)){
            /* 좋아요 하지 않은 게시물이면 좋아요 추가, true 반환 */
            User user = userRepository.findById(userId).orElse(null);
            Article article = articleRepository.findById(articleId).orElse(null);

            /* 좋아요 엔티티 생성 */
            UserLikeArticle userLikeArticle = new UserLikeArticle(user, article);
            userLikeArticleRepository.save(userLikeArticle);
            article.increaseLikeCount();

            return true;
        } else {
            /* 좋아요 한 게시물이면 좋아요 삭제 */
            Article article = articleRepository.findById(articleId).orElse(null);
            userLikeArticleRepository.deleteByArticleIdAndUserId(articleId, userId);
            article.decreaseLikeCount();

            return false;
        }
    }
}

