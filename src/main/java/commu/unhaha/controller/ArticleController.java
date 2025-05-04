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
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

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

    @GetMapping("/write/new")
    public String writeArticle(@ModelAttribute("article") WriteArticleForm writeArticleForm) {
        return "writeArticleForm";
    }

    @PostMapping("/write/new")
    public String saveArticle(@Validated @ModelAttribute("article") WriteArticleForm form, BindingResult bindingResult,
                              @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser,
                              RedirectAttributes rttr) {

        if (bindingResult.hasErrors()) {
            log.info("errors={}", bindingResult);
            return "writeArticleForm";
        }
        User user = userRepository.findByEmail(loginUser.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Long articleId = articleService.createArticle(form, user);
        rttr.addAttribute("articleId", articleId);
        return "redirect:/articles/{articleId}";
    }

    @GetMapping("/new")
    public String allArticles(Model model, @RequestParam(value = "page", defaultValue = "1") int page,
                               String searchType, String keyword) {
        int jpaPage = page - 1;
        Page<ArticlesDto> articles = (keyword == null)
                ? articleService.pageList(jpaPage)
                : articleService.searchPageList(jpaPage, keyword, searchType);

        processArticles(articles);

        PageInfo pageInfo = extractPageInfo(articles);

        model.addAttribute("articles", articles);
        model.addAttribute("start", pageInfo.start);
        model.addAttribute("end", pageInfo.end);
        model.addAttribute("maxPage", pageInfo.maxPage);

        if (keyword != null) {
            model.addAttribute("searchType", searchType);
            model.addAttribute("keyword", keyword);
        }

        return "new";
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
        return new PageInfo(start, end, maxPage);
    }

    private static class PageInfo {
        int start;
        int end;
        int maxPage;

        PageInfo(int start, int end, int maxPage) {
            this.start = start;
            this.end = end;
            this.maxPage = maxPage;
        }
    }

    @GetMapping("/articles/{articleId}")
    public String article(@PathVariable Long articleId, Model model, HttpServletRequest request,
                          @RequestParam(value = "page", defaultValue = "1") int page,
                          @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser) {
        int jpaPage = page - 1;
        ArticleDto articleDto;
        boolean like = false;

        if (loginUser == null) {
            log.info("비회원 조회");
            String clientIp = extractClientIp(request);
            log.info(">>>> Result : IP Address : " + clientIp);

            articleDto = articleService.noneMemberView(articleId, clientIp);
        } else {
            log.info("회원 조회");
            String email = loginUser.getEmail();
            Long userId = getUserIdByEmail(email);

            articleDto = articleService.memberView(articleId, email);
            like = articleService.findLike(articleId, userId);
        }

        setDateTime(articleDto);
        // 댓글 페이징 처리
        Page<CommentDto> commentPages = commentService.commentPageList(articleId, jpaPage);
        PageInfo pageInfo = extractPageInfo(commentPages);
        for (CommentDto commentDto : commentPages){
            setDateTimeRecursively(commentDto);
        }


        model.addAttribute("article", articleDto);
        model.addAttribute("comments", commentPages);
        model.addAttribute("start", pageInfo.start);
        model.addAttribute("end", pageInfo.end);
        model.addAttribute("maxPage", pageInfo.maxPage);
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("like", like);

        return "article";
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

    @GetMapping("/articles/{articleId}/edit")
    public String editForm(@PathVariable Long articleId, Model model) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글을 찾을 수 없습니다."));

        WriteArticleForm writeArticleForm = WriteArticleForm.builder()
                .board(article.getBoard())
                .title(article.getTitle())
                .content(article.getContent())
                .build();
        model.addAttribute("article", writeArticleForm);
        return "editArticleForm";
    }

    /** 게시글 수정 */
    @PostMapping("/articles/{articleId}/edit")
    public String edit(@PathVariable Long articleId, @Validated @ModelAttribute("article") WriteArticleForm form, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            log.info("errors={}", bindingResult);
            return "editArticleForm";
        }
        articleService.editArticle(articleId, form);
        return "redirect:/articles/{articleId}";
    }

    /** 게시글 삭제 */
    @PostMapping("/articles/{articleId}/delete")
    public String deleteArticle(@PathVariable Long articleId, RedirectAttributes redirectAttributes) {
        try {
            articleService.deleteArticle(articleId);
            List<ArticleImage> relatedImages = articleImageRepository.findByArticleId(articleId);
            redirectAttributes.addFlashAttribute("message", "게시글이 성공적으로 삭제되었습니다.");
            return "redirect:/new"; // 게시글 목록 페이지로 리다이렉트
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "해당 게시글을 찾을 수 없습니다.");
            return "redirect:/new";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "게시글 삭제 중 오류가 발생했습니다.");
            return "redirect:/new";
        }
    }

    /** ckeditor 이미지 업로드 */
    @ResponseBody
    @PostMapping("/images/article")
    public Map<String, Object> uploadArticleImage(MultipartHttpServletRequest request) throws IOException {
        Map<String, Object> response = new HashMap<>();

        MultipartFile uploadFile = request.getFile("upload");

        UploadFile uploaded = gcsFileStore.storeArticleImage(uploadFile);
        //ArticleImage에 임시파일로 저장
        ArticleImage articleImage = ArticleImage.createTemp(uploaded.getStoreFileUrl());
        articleImageRepository.save(articleImage);

        response.put("uploaded", true);
        response.put("url", uploaded.getStoreFileUrl()); // 이게 GCS URL

        return response;
    }


    @PostMapping("/like/{articleId}")
    public String like(@PathVariable Long articleId, @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser) {

        Long member_id = userRepository.findByEmail(loginUser.getEmail()).orElse(null).getId();

        articleService.saveLike(articleId, member_id);

        return "redirect:/articles/{articleId}";
    }

    private void setDateTime(ArticleDto articleDto) {
        LocalDateTime createdDate = articleDto.getCreatedDate();
        LocalDateTime now = LocalDateTime.now();
        String dateTime = articleService.calDateTime(createdDate, now);
        articleDto.setDateTime(dateTime);
    }

    /**
     * 테스트용 데이터 추가
     */
    @PostConstruct
    public void init() {
        User user = userRepository.save(new User("길동", "홍길동", "222@naver.com", Role.USER, new UploadFile("userImage", "userImage")));
        User user2 = userRepository.save(new User("로니콜먼", "로니콜먼", "333@naver.com", Role.USER, new UploadFile("userImage", "userImage")));
        for (int i = 0; i < 400; i++) {
            articleRepository.save(new Article("보디빌딩", "오운완" + i, "오늘도 운동 완료", user, 0, 0));
        }
        commentService.createComment(user2, 400L, "Light Weight BABY!", null);
        commentService.createComment(user2, 400L, "Easy Weight BABY!", 1L);
//        articleRepository.save(new Article("보디빌딩", "오운완", "오늘도 운동 완료", user));
//        articleRepository.save(new Article("파워리프팅", "3대 500달성", "힘들다", user));
//        articleRepository.save(new Article("파워리프팅", "3대 500달성", "힘들다", user));

    }
}
