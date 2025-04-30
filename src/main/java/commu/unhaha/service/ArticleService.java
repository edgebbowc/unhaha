package commu.unhaha.service;

import commu.unhaha.controller.SessionConst;
import commu.unhaha.domain.Article;
import commu.unhaha.domain.ArticleImage;
import commu.unhaha.domain.User;
import commu.unhaha.domain.UserLikeArticle;
import commu.unhaha.dto.ArticleDto;
import commu.unhaha.dto.ArticlesDto;
import commu.unhaha.dto.SessionUser;
import commu.unhaha.dto.WriteArticleForm;
import commu.unhaha.file.GCSFileStore;
import commu.unhaha.repository.ArticleImageRepository;
import commu.unhaha.repository.ArticleRepository;
import commu.unhaha.repository.UserLikeArticleRepository;
import commu.unhaha.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final RedisService redisService;
    private final UserLikeArticleRepository userLikeArticleRepository;
    private final UserRepository userRepository;
    private final ArticleImageRepository articleImageRepository;
    private final GCSFileStore gcsFileStore;

    //Article 작성
    public Long createArticle(WriteArticleForm form, User user) {
        Article article = Article.builder()
                .board(form.getBoard())
                .title(form.getTitle())
                .content(form.getContent())
                .user(user)
                .viewCount(0)
                .likeCount(0)
                .build();

        articleRepository.save(article);

        // 이미지 연결
        Set<String> imageUrls = extractImageUrls(form.getContent());
        List<ArticleImage> images = articleImageRepository.findByUrlIn(imageUrls);

        for (ArticleImage image : images) {
            image.attachToArticle(article);
        }
        articleImageRepository.saveAll(images);

        return article.getId();
    }
    //Article 수정
    public void editArticle(Long articleId, WriteArticleForm form) {
        Article article = validateAndGetArticle(articleId);

        // 기존 이미지 정리
        Set<String> newImageUrls = extractImageUrls(form.getContent());
        List<ArticleImage> oldImages = articleImageRepository.findByArticleId(articleId);

        Set<String> oldImageUrls = oldImages.stream()
                .map(ArticleImage::getUrl)
                .collect(Collectors.toSet());

        Set<String> toUnlink = new HashSet<>(oldImageUrls);
        toUnlink.removeAll(newImageUrls);

        for (ArticleImage image : oldImages) {
            if (toUnlink.contains(image.getUrl())) {
                image.markAsTemp();
            }
        }

        article.changeArticle(form.getBoard(), form.getTitle(), form.getContent());
    }

    //Article 삭제
    public void deleteArticle(Long articleId) {
        validateArticle(articleId);
        List<ArticleImage> relatedImages = articleImageRepository.findByArticleId(articleId);

        for (ArticleImage image : relatedImages) {
            try {
                gcsFileStore.deleteFile(image.getUrl());
            } catch (Exception e) {
                log.warn("GCS 이미지 삭제 실패: {}", image.getUrl(), e);

            }
        }

        articleImageRepository.deleteAll(relatedImages);
        articleRepository.deleteById(articleId);
    }

    // 페이징
    public Page<ArticlesDto> pageList(int page) {
        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "id"));
        Page<Article> articlePage = articleRepository.findAll(pageable);
        Page<ArticlesDto> articleDtoPage = articlePage.map(article -> new ArticlesDto(article));
        return articleDtoPage;
    }

    // 제목, 제목+내용 닉네임 검색 페이징
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
            case "nickName" :
                Page<Article> articlePageN = articleRepository.findByNicknamePaging(keyword, pageable);
                Page<ArticlesDto> articleDtoPageN = articlePageN.map(article -> new ArticlesDto(article));
                return articleDtoPageN;
        }
        return null;
    }

    public void addViewCount(Long articleId) {
        articleRepository.increaseViews(articleId);
    }

    public ArticleDto noneMemberView(Long articleId, String clientAddress) {
        Article article = validateAndGetArticle(articleId);

        if (redisService.isFirstIpRequest(clientAddress, articleId)) {
            addViewCount(articleId);
        }

        return new ArticleDto(article);
    }

    public ArticleDto memberView(Long articleId, String email) {
        Article article = validateAndGetArticle(articleId);

        if (redisService.isFirstMemberRequest(email, articleId)) {
            addViewCount(articleId);
        }

        return new ArticleDto(article);
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
            Article article = validateAndGetArticle(articleId);

            /* 좋아요 엔티티 생성 */
            UserLikeArticle userLikeArticle = new UserLikeArticle(user, article);
            userLikeArticleRepository.save(userLikeArticle);
            article.increaseLikeCount();

            return true;
        } else {
            /* 좋아요 한 게시물이면 좋아요 삭제 */
            Article article = validateAndGetArticle(articleId);
            userLikeArticleRepository.deleteByArticleIdAndUserId(articleId, userId);
            article.decreaseLikeCount();

            return false;
        }
    }

    public void validateArticle(Long articleId) {
        if (!articleRepository.existsById(articleId)) {
            throw new IllegalArgumentException("해당 게시글이 존재하지 않습니다.");
        }
    }

    public Article validateAndGetArticle(Long articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));
    }

    private Set<String> extractImageUrls(String content) {
        Set<String> urls = new HashSet<>();
        Document doc = Jsoup.parse(content); // HTML 파싱
        Elements imgTags = doc.select("img"); // 모든 <img> 태그 선택

        for (Element img : imgTags) {
            String src = img.attr("src"); // src 속성값 가져오기
            if (!src.isEmpty()) {
                urls.add(src);
            }
        }

        return urls;
    }
}

