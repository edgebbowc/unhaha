package commu.unhaha.restapi;
import commu.unhaha.controller.SessionConst;
import commu.unhaha.domain.BoardType;
import commu.unhaha.domain.User;
import commu.unhaha.dto.*;
import commu.unhaha.dto.restapidto.HomeResponse;
import commu.unhaha.repository.UserRepository;
import commu.unhaha.service.ArticleService;
import commu.unhaha.service.CommentService;
import commu.unhaha.util.TimeAgoFormatter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api")
@Tag(name = "게시글 API", description = "게시글 관련 CRUD API")
public class RestArticleController implements ArticleApiDocs {
    private final ArticleService articleService;
    private final UserRepository userRepository;
    private final CommentService commentService;

    /**
     * 홈페이지 (인기글 목록) API
     */
    @GetMapping("/home")
    public ResponseEntity<HomeResponse> home(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "searchType", required = false) String searchType,
            @RequestParam(value = "keyword", required = false) String keyword) {

        Page<ArticlesDto> articles = articleService.findPopularArticles(page - 1, searchType, keyword);
        processArticles(articles);
        PageInfo pageInfo = extractPageInfo(articles);

        HomeResponse response = new HomeResponse();
        response.setArticles(articles);
        response.setPageInfo(pageInfo);
        response.setSearchType(searchType);
        response.setKeyword(keyword);

        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 생성 API
     */
    @PostMapping("/articles/{boardType}")
    public ResponseEntity<Map<String, Object>> createArticle(
            @PathVariable String boardType,
            @Valid @RequestBody WriteArticleForm form,
            @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser) {

        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인이 필요합니다."));
        }
        if (!isValidBoardType(boardType)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "잘못된 게시판 타입입니다: " + boardType));
        }

        try {
            User user = userRepository.findByEmail(loginUser.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            Long articleId = articleService.createArticle(form, user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("articleId", articleId);
            response.put("message", "게시글이 성공적으로 작성되었습니다.");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "게시글 작성 중 오류가 발생했습니다."));
        }
    }

    /**
     * 게시글 수정 API
     */
    @PutMapping("/articles/{boardType}/{articleId}")
    public ResponseEntity<Map<String, Object>> updateArticle(
            @PathVariable String boardType,
            @PathVariable Long articleId,
            @Valid @RequestBody WriteArticleForm form,
            @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser) {

        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인이 필요합니다."));
        }
        if (!isValidBoardType(boardType)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "잘못된 게시판 타입입니다: " + boardType));
        }

        try {
            articleService.editArticle(articleId, form, loginUser.getEmail());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("articleId", articleId);
            response.put("message", "게시글이 성공적으로 수정되었습니다.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {  // SecurityException 별도 처리
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "게시글 수정 중 오류가 발생했습니다."));
        }
    }

    /**
     * 게시글 삭제 API
     */
    @DeleteMapping("/articles/{boardType}/{articleId}")
    public ResponseEntity<Map<String, Object>> deleteArticle(
            @PathVariable String boardType,
            @PathVariable Long articleId,
            @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser) {

        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인이 필요합니다."));
        }
        if (!isValidBoardType(boardType)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "잘못된 게시판 타입입니다: " + boardType));
        }


        try {
            articleService.deleteArticle(articleId, loginUser.getEmail());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("articleId", articleId);
            response.put("message", "게시글이 성공적으로 삭제되었습니다.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {  // SecurityException 먼저 처리
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "게시글 삭제 중 오류가 발생했습니다."));
        }
    }

    /**
     * 게시판 목록 API
     */
    @GetMapping("/articles/{boardType}")
    public ResponseEntity<Map<String, Object>> getArticleList(
            @PathVariable String boardType,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "searchType", required = false) String searchType,
            @RequestParam(value = "keyword", required = false) String keyword) {

        if (!isValidBoardType(boardType)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "잘못된 게시판 타입입니다: " + boardType));
        }

        try {
            BoardType type = BoardType.fromString(boardType);
            Page<ArticlesDto> articles = articleService.findArticles(boardType, page - 1, searchType, keyword);

            processArticles(articles);
            PageInfo pageInfo = extractPageInfo(articles);

            Map<String, Object> response = new HashMap<>();
            response.put("articles", articles);
            response.put("pageInfo", pageInfo);
            response.put("boardType", type.getType());
            response.put("boardTitle", type.getTitle());
            response.put("searchType", searchType);
            response.put("keyword", keyword);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "게시글 목록 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 게시글 상세 조회 API
     */
    @GetMapping("/articles/{boardType}/{articleId}")
    public ResponseEntity<Map<String, Object>> getArticleDetail(
            @PathVariable String boardType,
            @PathVariable Long articleId,
            @RequestParam(value = "page", required = false) Integer page,
            @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser,
            HttpServletRequest request) {

        if (!isValidBoardType(boardType)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "잘못된 게시판 타입입니다: " + boardType));
        }

        try {
            // 게시글 데이터 처리
            Map<String, Object> articleData = processArticleDataForApi(articleId, loginUser, request);

            ArticleDto articleDto = (ArticleDto) articleData.get("article");
            // 댓글 데이터 처리
            Map<String, Object> commentData = processCommentDataForApi(articleId, page);

            // 네비게이션 데이터 처리
            Map<String, Object> navigationData = processNavigationDataForApi(boardType, articleId, articleDto);

            Map<String, Object> response = new HashMap<>();
            response.put("article", articleDto);
            response.put("comments", commentData.get("comments"));
            response.put("commentPageInfo", commentData.get("pageInfo"));
            response.put("navigation", navigationData);
            response.put("boardType", boardType);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "게시글 상세 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 게시글 좋아요 API
     */
    @PostMapping("/articles/{boardType}/{articleId}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @PathVariable String boardType,
            @PathVariable Long articleId,
            @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser) {

        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인이 필요합니다."));
        }

        if (!isValidBoardType(boardType)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "잘못된 게시판 타입입니다: " + boardType));
        }


        try {
            boolean liked = articleService.saveLike(articleId, loginUser.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("liked", liked);
            response.put("articleId", articleId);
            response.put("message", liked ? "좋아요가 추가되었습니다." : "좋아요가 취소되었습니다.");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "좋아요 처리 중 오류가 발생했습니다."));
        }
    }

    private Map<String, Object> processArticleDataForApi(Long articleId, SessionUser loginUser, HttpServletRequest request) {
        ArticleDto articleDto;
        boolean articleLike = false;
        List<Long> likedCommentIds = Collections.emptyList();

        if (loginUser == null) {
            log.info("비회원 조회");
            String clientIp = extractClientIp(request);
            articleDto = articleService.noneMemberView(articleId, clientIp);
        } else {
            log.info("회원 조회");
            String email = loginUser.getEmail();
            Long userId = getUserIdByEmail(email);
            articleDto = articleService.memberView(articleId, email);
            articleLike = articleService.findLike(articleId, userId);
            likedCommentIds = commentService.findLikedCommentsByUser(userId);
        }

        // 시간 포맷 설정
        setDateTime(articleDto);

        Map<String, Object> result = new HashMap<>();
        result.put("article", articleDto);
        result.put("isLiked", articleLike);
        result.put("likedCommentIds", likedCommentIds);
        result.put("isLoggedIn", loginUser != null);

        return result;

    }

    private Map<String, Object> processCommentDataForApi(Long articleId, Integer page) {
        int jpaPage = commentService.calculatePageIndex(articleId, page);
        Page<CommentDto> commentPages = commentService.commentPageList(articleId, jpaPage);

        commentPages.getContent().forEach(commentDto -> {
            setCommentImageUrls(commentDto);
            setDateTimeRecursively(commentDto);
        });

        PageInfo pageInfo = extractPageInfo(commentPages);
        long rootCommentCount = commentService.countRootComments(articleId);

        Map<String, Object> result = new HashMap<>();
        result.put("comments", commentPages.getContent());
        result.put("pageInfo", pageInfo);
        result.put("shouldShowPagination", rootCommentCount > commentPages.getSize());

        return result;
    }

    private Map<String, Object> processNavigationDataForApi(String boardType, Long articleId, ArticleDto articleDto) {
        ArticleDto prevArticle;
        ArticleDto nextArticle;

        switch (boardType) {
            case "best":
                prevArticle = articleService.findPrevPopularArticle(articleDto.getLikeAchievedAt());
                nextArticle = articleService.findNextPopularArticle(articleDto.getLikeAchievedAt());
                break;
            case "bodybuilding":
                prevArticle = articleService.findPrevBoardArticle(articleId, "보디빌딩");
                nextArticle = articleService.findNextBoardArticle(articleId, "보디빌딩");
                break;
            case "powerlifting":
                prevArticle = articleService.findPrevBoardArticle(articleId, "파워리프팅");
                nextArticle = articleService.findNextBoardArticle(articleId, "파워리프팅");
                break;
            case "crossfit":
                prevArticle = articleService.findPrevBoardArticle(articleId, "크로스핏");
                nextArticle = articleService.findNextBoardArticle(articleId, "크로스핏");
                break;
            case "new":
            default:
                prevArticle = articleService.findPrevArticle(articleId);
                nextArticle = articleService.findNextArticle(articleId);
                break;
        }

        Map<String, Object> navigation = new HashMap<>();
        navigation.put("prevArticle", prevArticle);
        navigation.put("nextArticle", nextArticle);
        navigation.put("type", boardType);
        navigation.put("basePath", "/" + boardType);

        return navigation;
    }

    /**
     * 게시글 목록 후처리 (썸네일, 시간 포맷)
     */
    public void processArticles(Page<ArticlesDto> articles) {
        for (ArticlesDto articleDto : articles) {
            Document doc = Jsoup.parse(articleDto.getContent());
            Element img = doc.selectFirst("img");
            if (img != null) {
                articleDto.setThumb(img.attr("src"));
            }

            String dateTime = TimeAgoFormatter.format(articleDto.getCreatedDate(), LocalDateTime.now());
            articleDto.setDateTime(dateTime);
        }
    }

    /**
     * 페이지 정보 추출
     */
    public <T> PageInfo extractPageInfo(Page<T> page) {
        int maxPage = 5;
        int totalPages = page.getTotalPages();
        int start = (page.getNumber() / maxPage) * maxPage + 1;
        int end = (totalPages == 0) ? 1 : Math.min(start + maxPage - 1, totalPages);
        int currentPage = page.getNumber() + 1;

        return new PageInfo(start, end, maxPage, totalPages, currentPage);
    }

    /**
     * 게시글 시간 포맷 설정
     */
    public void setDateTime(ArticleDto articleDto) {
        LocalDateTime createdDate = articleDto.getCreatedDate();
        LocalDateTime now = LocalDateTime.now();
        String dateTime = TimeAgoFormatter.format(createdDate, now);
        articleDto.setDateTime(dateTime);
    }

    /**
     * 댓글 이미지 URL 설정 (재귀적)
     */
    public void setCommentImageUrls(CommentDto commentDto) {
        List<String> imageUrls = new ArrayList<>();
        String content = commentDto.getContent();

        Pattern pattern = Pattern.compile("!\\[이미지\\]\\((.*?)\\)");
        Matcher matcher = pattern.matcher(content);

        StringBuffer cleanContent = new StringBuffer();
        while (matcher.find()) {
            imageUrls.add(matcher.group(1));
            matcher.appendReplacement(cleanContent, "");
        }
        matcher.appendTail(cleanContent);

        commentDto.setContent(cleanContent.toString().trim());
        commentDto.setImageUrls(imageUrls);

        for (CommentDto child : commentDto.getChildren()) {
            setCommentImageUrls(child);
        }
    }

    /**
     * 댓글 시간 포맷 설정 (재귀적)
     */
    public void setDateTimeRecursively(CommentDto commentDto) {
        String dateTime = TimeAgoFormatter.format(commentDto.getCreatedDate(), LocalDateTime.now());
        commentDto.setDateTime(dateTime);

        for (CommentDto child : commentDto.getChildren()) {
            setDateTimeRecursively(child);
        }
    }

    /**
     * 게시판 타입 유효성 검증
     */
    public boolean isValidBoardType(String boardType) {
        return Arrays.asList("new", "best", "bodybuilding", "powerlifting", "crossfit").contains(boardType);
    }

    /**
     * URL에서 게시판 타입 추출
     */
    public String extractBoardType(String requestPath) {
        if (requestPath.contains("/best")) return "best";
        if (requestPath.contains("/bodybuilding")) return "bodybuilding";
        if (requestPath.contains("/powerlifting")) return "powerlifting";
        if (requestPath.contains("/crossfit")) return "crossfit";
        return "new";
    }

    private Long getUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "해당 이메일로 사용자를 찾을 수 없습니다."
                )).getId();
    }

    /**
     * 클라이언트 IP 추출
     */
    public String extractClientIp(HttpServletRequest request) {
        String ip = null;
        String[] headers = {
                "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            ip = request.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }
}
