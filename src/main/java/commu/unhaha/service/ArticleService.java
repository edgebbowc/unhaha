package commu.unhaha.service;

import commu.unhaha.controller.SessionConst;
import commu.unhaha.domain.*;
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
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    /**
     * Article 작성 메서드
     */
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

    /**
     * Article 수정 메서드
     */
    public void editArticle(Long articleId, WriteArticleForm form, String userEmail) {
        Article article = validateAndGetArticle(articleId);

        // 소유자 검증 추가
        if (!article.getUser().getEmail().equals(userEmail)) {
            throw new SecurityException("본인이 작성한 게시글만 수정할 수 있습니다.");
        }

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
        // 2. 새로 추가된 이미지 ACTIVE로 변경 (누락된 로직)
        Set<String> newlyAddedUrls = new HashSet<>(newImageUrls);
        newlyAddedUrls.removeAll(oldImageUrls); // 기존에 없던 새 URL만 추출

        if (!newlyAddedUrls.isEmpty()) {
            // TEMP 상태인 새 이미지들 조회
            List<ArticleImage> tempImages = articleImageRepository
                    .findByUrlInAndStatus(new ArrayList<>(newlyAddedUrls), ImageStatus.TEMP);

            // ACTIVE 상태로 변경 및 게시글 연결
            for (ArticleImage tempImage : tempImages) {
                tempImage.attachToArticle(article); // TEMP → ACTIVE 변경 + 게시글 연결
                log.debug("새 이미지를 게시글에 연결: {}", tempImage.getUrl());
            }
        }

        article.changeArticle(form.getBoard(), form.getTitle(), form.getContent());
    }

    /**
     * Article 삭제 메서드
     */
    public void deleteArticle(Long articleId, String userEmail) {
        Article article = validateAndGetArticle(articleId);

        // 소유자 검증
        if (!article.getUser().getEmail().equals(userEmail)) {
            throw new SecurityException("본인이 작성한 게시글만 삭제할 수 있습니다.");
        }

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

    /**
     * 보드타입 별 게시글 조회
     */
    public Page<ArticlesDto> findArticles(String boardType, int page, String searchType, String keyword) {
        if ("best".equals(boardType)) {
            return findPopularArticles(page, searchType, keyword);
        } else if ("bodybuilding".equals(boardType)) {
            return findBoardArticles("보디빌딩", page, searchType, keyword);
        } else if ("powerlifting".equals(boardType)) {
            return findBoardArticles("파워리프팅", page, searchType, keyword);
        } else if ("crossfit".equals(boardType)) {
            return findBoardArticles("크로스핏", page, searchType, keyword);
        } else {
            return findAllArticles(page, searchType, keyword);
        }
    }

    /**
     * 전체 게시글 조회 (검색 포함)
     */
    public Page<ArticlesDto> findAllArticles(int page, String searchType, String keyword) {
        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "id"));
        return articleRepository.findAllArticles(searchType, keyword, pageable);
    }

    /**
     * 인기 게시글 조회 (검색 포함)
     */
    public Page<ArticlesDto> findPopularArticles(int page, String searchType, String keyword) {
        Pageable pageable = PageRequest.of(page, 20);
        return articleRepository.findPopularArticles(searchType, keyword, pageable);
    }

    /**
     * 특정 게시판 조회 (검색 포함)
     */
    public Page<ArticlesDto> findBoardArticles(String boardType, int page, String searchType, String keyword) {
        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "id"));
        return articleRepository.findBoardArticles(boardType, searchType, keyword, pageable);
    }

    /**
     * 전체글에서 이전글 찾기
     */
    public ArticleDto findPrevArticle(Long currentId) {
        Article article = articleRepository.findTopByIdGreaterThanOrderByIdAsc(currentId);
        return article != null ? convertToDto(article) : null;
    }
    /**
     * 전체글에서 다음글 찾기
     */
    public ArticleDto findNextArticle(Long currentId) {
        Article article = articleRepository.findTopByIdLessThanOrderByIdDesc(currentId);
        return article != null ? convertToDto(article) : null;
    }

    /**
     * 인기글에서 이전글 찾기 (좋아요 달성시점 기준)
     */
    public ArticleDto findPrevPopularArticle(LocalDateTime likeAchievedAt) {
        Article article = articleRepository.findTopByLikeAchievedAtGreaterThanOrderByLikeAchievedAtAsc(likeAchievedAt);
        return article != null ? convertToDto(article) : null;
    }

    /**
     * 인기글에서 다음글 찾기 (좋아요 달성시점 기준)
     */
    public ArticleDto findNextPopularArticle(LocalDateTime likeAchievedAt) {
        Article article = articleRepository.findTopByLikeAchievedAtLessThanOrderByLikeAchievedAtDesc(likeAchievedAt);
        return article != null ? convertToDto(article) : null;
    }

    /**
     * boardName 게시글에서 이전글 찾기
     */
    public ArticleDto findPrevBoardArticle(Long currentId, String boardName) {
        Article article = articleRepository.findTopByIdGreaterThanAndBoardOrderByIdAsc(currentId, boardName);
        return article != null ? convertToDto(article) : null;
    }

    /**
     * boardName 게시글에서 다음글 찾기
     */
    public ArticleDto findNextBoardArticle(Long currentId, String boardName) {
        Article article = articleRepository.findTopByIdLessThanAndBoardOrderByIdDesc(currentId, boardName);
        return article != null ? convertToDto(article) : null;
    }

    /**
     * Article -> ArticleDto 변환 메서드
     */
    private ArticleDto convertToDto(Article article) {
        return article != null ? new ArticleDto(article) : null;
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

        Article article = validateAndGetArticle(articleId);
        if (article.getUser().getId().equals(userId)) {
            throw new SecurityException("자신의 게시글에는 운하하 할 수 없습니다");
        }
        /** 로그인한 유저가 해당 게시물을 좋아요 했는지 안 했는지 확인 **/
        if(!findLike(articleId, userId)){
            /* 좋아요 하지 않은 게시물이면 좋아요 추가, true 반환 */
            User user = userRepository.findById(userId).orElse(null);

            /* 좋아요 엔티티 생성 */
            UserLikeArticle userLikeArticle = new UserLikeArticle(user, article);
            userLikeArticleRepository.save(userLikeArticle);
            article.increaseLikeCount();

            return true;
        } else {
            /* 좋아요 한 게시물이면 좋아요 삭제 */
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
    /**
     * 사용자 이메일로 작성한 게시글 목록 조회
     */
    public List<ArticleDto> findArticlesByUserEmail(String email) {
        List<Article> articles = articleRepository.findByUserEmailOrderByCreatedDateDesc(email);
        return articles.stream()
                .map(ArticleDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 사용자가 좋아요한 게시글 목록 조회
     */
    public List<ArticleDto> findLikedArticlesByUserEmail(String email) {
        List<Article> likedArticles = articleRepository.findLikedArticlesByUserEmail(email);
        return likedArticles.stream()
                .map(ArticleDto::new)
                .collect(Collectors.toList());
    }
}

