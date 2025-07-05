package commu.unhaha.controller;

import commu.unhaha.domain.*;
import commu.unhaha.dto.*;
import commu.unhaha.file.GCSFileStore;
import commu.unhaha.repository.ArticleImageRepository;
import commu.unhaha.repository.ArticleRepository;
import commu.unhaha.repository.UserRepository;
import commu.unhaha.service.ArticleService;
import commu.unhaha.service.CommentService;
import commu.unhaha.util.TimeAgoFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static commu.unhaha.domain.BoardType.*;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ArticleController {
    private final ArticleService articleService;
    private final ArticleRepository articleRepository;
    private final ArticleImageRepository articleImageRepository;
    private final UserRepository userRepository;
    private final GCSFileStore gcsFileStore;
    private final CommentService commentService;

    /**
     * 홈페이지 (인기글 목록)
     */
    @GetMapping("/")
    public String home(Model model,
                       @RequestParam(value = "page", defaultValue = "1") int page,
                       @RequestParam(value = "searchType", required = false) String searchType,
                       @RequestParam(value = "keyword", required = false) String keyword) {

        Page<ArticlesDto> articles = articleService.findPopularArticles(page - 1, searchType, keyword);
        
        processArticles(articles);

        PageInfo pageInfo = extractPageInfo(articles);

        model.addAttribute("articles", articles);
        model.addAttribute("start", pageInfo.getStart());
        model.addAttribute("end", pageInfo.getEnd());
        model.addAttribute("maxPage", pageInfo.getMaxPage());
        model.addAttribute("currentPage", pageInfo.getCurrentPage());
        if (keyword != null) {
            model.addAttribute("searchType", searchType);
            model.addAttribute("keyword", keyword);
        }
        return "home";
    }

    /**
     * 게시글 작성 폼
     */
    @GetMapping("/write/{boardType}")
    public String writeArticle(@PathVariable String boardType,
                               @ModelAttribute("article") WriteArticleForm writeArticleForm,
                               Model model) {
        // 유효한 게시판 타입인지 검증
        if (!isValidBoardType(boardType)) {
            throw new IllegalArgumentException("Invalid board type: " + boardType);
        }

        String defaultBoard = mapBoardTypeToKorean(boardType);

        // URL에 따라 board 필드 자동 설정
        writeArticleForm.setBoard(defaultBoard);

        return "writeArticleForm";
    }

    private String mapBoardTypeToKorean(String boardType) {
        switch (boardType.toLowerCase()) {
            case "bodybuilding": return "보디빌딩";
            case "powerlifting": return "파워리프팅";
            case "crossfit": return "크로스핏";
            case "humor": return "유머";
            case "new":
            default: return ""; // 빈 값 = "게시판을 선택해 주세요"
        }
    }


    /**
     * 게시글 작성 저장
     */
    @PostMapping("/write/{boardType}")
    public String saveArticle(@PathVariable String boardType,
                              @Validated @ModelAttribute("article") WriteArticleForm form, BindingResult bindingResult,
                              @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser,
                              RedirectAttributes rttr) {
        // 유효한 게시판 타입인지 검증
        if (!isValidBoardType(boardType)) {
            throw new IllegalArgumentException("Invalid board type: " + boardType);
        }

        if (bindingResult.hasErrors()) {
            log.info("errors={}", bindingResult);
            return "writeArticleForm";
        }
        User user = userRepository.findByEmail(loginUser.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Long articleId = articleService.createArticle(form, user);
        rttr.addAttribute("articleId", articleId);
        return "redirect:/{boardType}/{articleId}";
    }

    /** 게시글 수정폼 조회 */
    @GetMapping("/{boardType}/{articleId}/edit")
    public String editForm(@PathVariable String boardType,
                           @PathVariable Long articleId, Model model) {
        // 유효한 게시판 타입인지 검증
        if (!isValidBoardType(boardType)) {
            throw new IllegalArgumentException("Invalid board type: " + boardType);
        }

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글을 찾을 수 없습니다."));

        // 게시판 타입 검증 추가
        String expectedBoardPath = titleToPath(article.getBoard());
        if (!expectedBoardPath.equals("/" + boardType)) {
            // 올바른 URL로 리다이렉트
            return "redirect:" + expectedBoardPath + "/" + articleId + "/edit";
        }
        WriteArticleForm writeArticleForm = WriteArticleForm.builder()
                .board(article.getBoard())
                .title(article.getTitle())
                .content(article.getContent())
                .build();
        model.addAttribute("article", writeArticleForm);
        return "editArticleForm";
    }

    /** 게시글 수정 */
    @PostMapping("/{boardType}/{articleId}/edit")
    public String edit(@PathVariable String boardType,
                       @PathVariable Long articleId,
                       @Validated @ModelAttribute("article") WriteArticleForm form,
                       @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser,
                       BindingResult bindingResult) {
        // 유효한 게시판 타입인지 검증
        if (!isValidBoardType(boardType)) {
            throw new IllegalArgumentException("Invalid board type: " + boardType);
        }

        if (bindingResult.hasErrors()) {
            log.info("errors={}", bindingResult);
            return "editArticleForm";
        }
        articleService.editArticle(articleId, form, loginUser.getEmail());
        return "redirect:/{boardType}/{articleId}";
    }

    /** 게시글 삭제 */
    @PostMapping("/{boardType}/{articleId}/delete")
    public String deleteArticle(@PathVariable String boardType,
                                @PathVariable Long articleId,
                                @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser,
                                RedirectAttributes redirectAttributes) {
        // 유효한 게시판 타입인지 검증
        if (!isValidBoardType(boardType)) {
            throw new IllegalArgumentException("Invalid board type: " + boardType);
        }

        try {
            articleService.deleteArticle(articleId, loginUser.getEmail());
            redirectAttributes.addFlashAttribute("message", "게시글이 성공적으로 삭제되었습니다.");
            return "redirect:/" + boardType; // 게시글 목록 페이지로 리다이렉트
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "해당 게시글을 찾을 수 없습니다.");
            return "redirect:/" + boardType;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "게시글 삭제 중 오류가 발생했습니다.");
            return "redirect:/" + boardType;
        }
    }

    /** ckeditor 이미지 업로드 */
    @ResponseBody
    @PostMapping("/articles/images")
    public Map<String, Object> uploadArticleImage(MultipartHttpServletRequest request) throws IOException {
        Map<String, Object> response = new HashMap<>();

        MultipartFile uploadFile = request.getFile("upload");

        UploadFile uploaded = gcsFileStore.storeArticleImage(uploadFile);
        //ArticleImage에 임시파일로 저장
        ArticleImage articleImage = ArticleImage.createTemp(uploaded.getStoreFileUrl());
        articleImageRepository.save(articleImage);

        response.put("uploaded", true);
        response.put("url", uploaded.getStoreFileUrl()); //  GCS URL

        return response;
    }

    /** 게시글 좋아요 */
    @PostMapping("/{boardType}/like/{articleId}")
    public String like(@PathVariable String boardType,
                       @PathVariable Long articleId,
                       @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser) {
        // 유효한 게시판 타입인지 검증
        if (!isValidBoardType(boardType)) {
            throw new IllegalArgumentException("Invalid board type: " + boardType);
        }
        Long member_id = userRepository.findByEmail(loginUser.getEmail()).orElse(null).getId();

        articleService.saveLike(articleId, member_id);

        return "redirect:/{boardType}/{articleId}";
    }

    /**
     * 게시판 목록
     */
    @GetMapping({"/new", "/best", "/bodybuilding", "/powerlifting", "/crossfit", "/humor"})
    public String listArticles(Model model,
                               HttpServletRequest request,
                               @RequestParam(value = "page", defaultValue = "1") int page,
                               @RequestParam(value = "searchType", required = false) String searchType,
                               @RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "result", required = false) String result) {

        String boardType = extractBoardType(request.getRequestURI());

        BoardType type = fromString(boardType);
        int jpaPage = page - 1;

        Page<ArticlesDto> articles = articleService.findArticles(boardType, jpaPage, searchType, keyword);

        processArticles(articles);
        PageInfo pageInfo = extractPageInfo(articles);

        model.addAttribute("articles", articles);
        model.addAttribute("start", pageInfo.getStart());
        model.addAttribute("end", pageInfo.getEnd());
        model.addAttribute("currentPage", pageInfo.getCurrentPage());
        model.addAttribute("maxPage", pageInfo.getMaxPage());
        model.addAttribute("boardType", type.getType());
        model.addAttribute("boardTitle", type.getTitle());

        if (keyword != null) {
            model.addAttribute("searchType", searchType);
            model.addAttribute("keyword", keyword);
        }

        return "articles";
    }

    /**
     * 게시판별 상세 게시글
     */
    @GetMapping({"/new/{articleId}", "/best/{articleId}", "/bodybuilding/{articleId}",
            "/powerlifting/{articleId}", "/crossfit/{articleId}", "/humor/{articleId}"})
    public String articleDetail(@PathVariable Long articleId,
                                Model model,
                                HttpServletRequest request,
                                @RequestParam(value = "page", required = false) Integer listPage, // 게시글 목록 페이지
                                @RequestParam(value = "comment", required = false) Integer commentPage,  // 댓글 페이지
                                @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser) {

        String boardType = extractBoardType(request.getRequestURI());

        // 게시글 데이터 처리
        ArticleDto articleDto = processArticleData(articleId, loginUser, request, model);

        // 게시판 타입 일치 여부 검증
        if (!isValidBoardMatch(boardType, articleDto)) {
            // 올바른 URL로 리다이렉트
            String correctPath = articleDto.getBoardPath() + "/" + articleId;
            if (listPage != null) {
                correctPath += "?page=" + listPage;
            }
            return "redirect:" + correctPath;
        }

        // 댓글 데이터 처리
        processCommentData(articleId, commentPage, model);

        // 네비게이션 데이터 처리
        processNavigationData(boardType, articleId, articleDto, model);

        model.addAttribute("listPage", listPage != null ? listPage : 1);
        return "article";
    }

    private boolean isValidBoardMatch(String urlBoardType, ArticleDto articleDto) {
        if (urlBoardType.equals("new")) return true;
        else if(urlBoardType.equals("best")) return true;
        String articleBoardPath = articleDto.getBoardPath();
        String expectedPath = "/" + urlBoardType;

        // URL의 게시판 타입과 실제 게시글의 게시판이 일치하는지 확인
        return articleBoardPath.equals(expectedPath);
    }

    /**
     * 게시글 데이터 처리
     */
    private ArticleDto processArticleData(Long articleId, SessionUser loginUser, HttpServletRequest request, Model model) {
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

        setDateTime(articleDto);

        model.addAttribute("article", articleDto);
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("like", articleLike);
        model.addAttribute("likedCommentIds", likedCommentIds);

        return articleDto;
    }

    /**
     * 댓글 데이터 처리
     */
    private void processCommentData(Long articleId, Integer page, Model model) {
        int jpaPage = commentService.calculatePageIndex(articleId, page);
        Page<CommentDto> commentPages = commentService.commentPageList(articleId, jpaPage);

        // 댓글 후처리 - Stream API 활용
        commentPages.getContent().forEach(commentDto -> {
            setCommentImageUrls(commentDto);
            setDateTimeRecursively(commentDto);
        });

        PageInfo pageInfo = extractPageInfo(commentPages);
        long rootCommentCount = commentService.countRootComments(articleId);

        model.addAttribute("comments", commentPages);
        model.addAttribute("start", pageInfo.getStart());
        model.addAttribute("end", pageInfo.getEnd());
        model.addAttribute("maxPage", pageInfo.getMaxPage());
        model.addAttribute("totalPages", pageInfo.getTotalPages());
        model.addAttribute("currentCommentPage", pageInfo.getCurrentPage());
        model.addAttribute("shouldShowPagination", rootCommentCount > commentPages.getSize());
    }

    /**
     * 이전글, 다음글 처리
     */
    private void processNavigationData(String boardType, Long articleId, ArticleDto articleDto, Model model) {
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
            case "humor":
                prevArticle = articleService.findPrevBoardArticle(articleId, "유머");
                nextArticle = articleService.findNextBoardArticle(articleId, "유머");
                break;
            case "new":
            default:
                prevArticle = articleService.findPrevArticle(articleId);
                nextArticle = articleService.findNextArticle(articleId);
                break;
        }

        model.addAttribute("prevArticle", prevArticle);
        model.addAttribute("nextArticle", nextArticle);
        model.addAttribute("type", boardType);
        model.addAttribute("basePath", "/" + boardType);
    }

    private String extractBoardType(String requestPath) {
        if (requestPath.contains("/best")) return "best";
        if (requestPath.contains("/bodybuilding")) return "bodybuilding";
        if (requestPath.contains("/powerlifting")) return "powerlifting";
        if (requestPath.contains("/crossfit")) return "crossfit";
        if (requestPath.contains("/humor")) return "humor";
        return "new";
    }

    // 컨트롤러 또는 서비스 레이어에서
    private void setCommentImageUrls(CommentDto commentDto) {
        // 이미지 URL 추출 (마크다운 형식: ![이미지](URL) 에서 URL 부분 추출)
        List<String> imageUrls = new ArrayList<>();
        String content = commentDto.getContent();

        // 마크다운 이미지 패턴 매칭
        Pattern pattern = Pattern.compile("!\\[이미지\\]\\((.*?)\\)");
        Matcher matcher = pattern.matcher(content);

        // 이미지 URL 추출 및 원본 내용에서 제거
        StringBuffer cleanContent = new StringBuffer();
        while (matcher.find()) {
            imageUrls.add(matcher.group(1));
            matcher.appendReplacement(cleanContent, ""); // 이미지 마크다운 제거
        }
        matcher.appendTail(cleanContent);

        commentDto.setContent(cleanContent.toString().trim());
        commentDto.setImageUrls(imageUrls);

        for (CommentDto child : commentDto.getChildren()) {
            setCommentImageUrls(child);
        }
    }

    private void setDateTimeRecursively(CommentDto commentDto) {
        String dateTime = TimeAgoFormatter.format(commentDto.getCreatedDate(), LocalDateTime.now());
        commentDto.setDateTime(dateTime);

        for (CommentDto child : commentDto.getChildren()) {
            setDateTimeRecursively(child);
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String ip = null;
        String[] headers = {
                "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            ip = request.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim(); // 첫 번째 IP 추출
                }
                log.info(">>>> {} : {}", header, ip);
                return ip;
            }
        }

        ip = request.getRemoteAddr();
        log.info(">>>> RemoteAddr : {}", ip);
        return ip;
    }

    private Long getUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "해당 이메일로 사용자를 찾을 수 없습니다."
                )).getId();
    }

    private void processArticles(Page<ArticlesDto> articles) {
        for (ArticlesDto articleDto : articles) {
            Document doc = Jsoup.parse(articleDto.getContent());
            Element img = doc.selectFirst("img");
            if (img != null) {
                articleDto.setThumb(img.attr("src"));
            }

            String dateTime = TimeAgoFormatter.format((articleDto.getCreatedDate()), LocalDateTime.now());
            articleDto.setDateTime(dateTime);
        }
    }

    private <T> PageInfo extractPageInfo(Page<T> page) {
        int maxPage = 5;
        int totalPages = page.getTotalPages();
        int start = (page.getNumber() / maxPage) * maxPage + 1;
        int end = (totalPages == 0) ? 1 : Math.min(start + maxPage - 1, totalPages);
        int currentPage = page.getNumber() + 1;

        return new PageInfo(start, end, maxPage, totalPages, currentPage);
    }

    private void setDateTime(ArticleDto articleDto) {
        LocalDateTime createdDate = articleDto.getCreatedDate();
        LocalDateTime now = LocalDateTime.now();
        String dateTime = TimeAgoFormatter.format(createdDate, now);
        articleDto.setDateTime(dateTime);
    }

    private boolean isValidBoardType(String boardType) {
        return Arrays.asList("new", "best", "bodybuilding", "powerlifting", "crossfit", "humor").contains(boardType);
    }
}
