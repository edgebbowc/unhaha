package commu.unhaha.controller;

import commu.unhaha.domain.Article;
import commu.unhaha.domain.Role;
import commu.unhaha.domain.UploadFile;
import commu.unhaha.domain.User;
import commu.unhaha.dto.ArticleDto;
import commu.unhaha.dto.ArticlesDto;
import commu.unhaha.dto.SessionUser;
import commu.unhaha.dto.WriteArticleForm;
import commu.unhaha.file.FileStore;
import commu.unhaha.repository.ArticleRepository;
import commu.unhaha.repository.UserRepository;
import commu.unhaha.service.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ArticleController {

    private final ArticleService articleService;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final FileStore fileStore;

    @GetMapping("/write/new")
    public String writeArticle(@ModelAttribute("article") WriteArticleForm writeArticleForm) {
        return "writeArticleForm";
    }

    @PostMapping("/write/new")
    public String saveArticle(@Validated @ModelAttribute("article") WriteArticleForm writeArticleForm, BindingResult bindingResult,
                              @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser,
                              RedirectAttributes rttr) {
        User findUser = userRepository.findByEmail(loginUser.getEmail()).orElse(null);
        writeArticleForm.setUser(findUser);
        if (bindingResult.hasErrors()) {
            log.info("errors={}", bindingResult);
            return "writeArticleForm";
        }
        Article article = Article.builder()
                .board(writeArticleForm.getBoard())
                .title(writeArticleForm.getTitle())
                .content(writeArticleForm.getContent())
                .user(writeArticleForm.getUser())
                .viewCount(0)
                .likeCount(0)
                .build();
        articleRepository.save(article);
        rttr.addAttribute("articleId", article.getId());
        return "redirect:/articles/{articleId}";
    }

    @GetMapping("/new")
    public String allArticles(Model model, @RequestParam(value = "page", defaultValue = "1") int page,
                              @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable, String searchType, String keyword) {
        int jpaPage = page - 1;
        if (keyword == null) {
            Page<ArticlesDto> articles = articleService.pageList(jpaPage);
            int totalPages = articles.getTotalPages();
            int maxPage = 5; //페이지 1~5, 6~10
            int start = (articles.getNumber() / maxPage) * maxPage + 1; //start = 1, 6, 11
            int end = totalPages == 0 ? 1 : (start + (maxPage - 1) < totalPages ? start + (maxPage - 1) : totalPages); //end= 5, 10, 15
            Iterator<ArticlesDto> iterator = articles.iterator();
            while (iterator.hasNext()) {
                ArticlesDto articleDto = iterator.next();
                Document doc = Jsoup.parse(articleDto.getContent());
                if (doc.selectFirst("img") != null) {
                    String src = doc.selectFirst("img").attr("src");
                    articleDto.setThumb(src);
                }
                LocalDateTime createdDate = articleDto.getCreatedDate();
                LocalDateTime now = LocalDateTime.now();
                String dateTime = articleService.calDateTime(createdDate, now);
                articleDto.setDateTime(dateTime);
            }
            model.addAttribute("articles", articles);
            model.addAttribute("start", start);
            model.addAttribute("end", end);
            model.addAttribute("maxPage", maxPage);
        } else {
            Page<ArticlesDto> articles = articleService.searchPageList(jpaPage, keyword, searchType);
            int totalPages = articles.getTotalPages();
            int maxPage = 5; //페이지 1~5, 6~10
            int start = (articles.getNumber() / maxPage) * maxPage + 1; //start = 1, 6, 11
            int end = totalPages == 0 ? 1 : (start + (maxPage - 1) < totalPages ? start + (maxPage - 1) : totalPages); //end= 5, 10, 15
            Iterator<ArticlesDto> iterator = articles.iterator();
            while (iterator.hasNext()) {
                ArticlesDto articleDto = iterator.next();
                Document doc = Jsoup.parse(articleDto.getContent());
                if (doc.selectFirst("img") != null) {
                    String src = doc.selectFirst("img").attr("src");
                    articleDto.setThumb(src);
                }
                LocalDateTime createdDate = articleDto.getCreatedDate();
                LocalDateTime now = LocalDateTime.now();
                String dateTime = articleService.calDateTime(createdDate, now);
                articleDto.setDateTime(dateTime);
            }
            model.addAttribute("articles", articles);
            model.addAttribute("start", start);
            model.addAttribute("end", end);
            model.addAttribute("maxPage", maxPage);
            model.addAttribute("searchType", searchType);
            model.addAttribute("keyword", keyword);
        }
        return "new";
    }

    @GetMapping("/articles/{articleId}")
    public String article(@PathVariable Long articleId, Model model, HttpServletRequest request,
                          @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser) {
        boolean like = false;

        if (loginUser == null) {
            log.info("비회원 조회");
            String clientAddress = request.getHeader("X-Forwarded-For");
            if (clientAddress == null) {
                clientAddress = request.getHeader("Proxy-Client-IP");
                log.info(">>>> Proxy-Client-IP : " + clientAddress);
            }
            if (clientAddress == null) {
                clientAddress = request.getHeader("WL-Proxy-Client-IP"); // 웹로직
                log.info(">>>> WL-Proxy-Client-IP : " + clientAddress);
            }
            if (clientAddress == null) {
                clientAddress = request.getHeader("HTTP_CLIENT_IP");
                log.info(">>>> HTTP_CLIENT_IP : " + clientAddress);
            }
            if (clientAddress == null) {
                clientAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
                log.info(">>>> HTTP_X_FORWARDED_FOR : " + clientAddress);
            }
            if (clientAddress == null) {
                clientAddress = request.getRemoteAddr();
            }

            log.info(">>>> Result : IP Address : "+ clientAddress);
            ArticleDto articleDto = articleService.noneMemberView(articleId, clientAddress);
            setDateTime(articleDto);
            model.addAttribute("article", articleDto);
            model.addAttribute("loginUser", loginUser);
            model.addAttribute("like", like);
        } else {
            log.info("회원 조회");

            ArticleDto articleDto = articleService.memberView(articleId, loginUser.getEmail());
            setDateTime(articleDto);
            String email = loginUser.getEmail();
            Long userId = userRepository.findByEmail(email).orElse(null).getId();
            like = articleService.findLike(articleId, userId);

            model.addAttribute("article", articleDto);
            model.addAttribute("loginUser", loginUser);
            model.addAttribute("like", like);
        }
        return "article";
    }

    @GetMapping("/articles/{articleId}/edit")
    public String editForm(@PathVariable Long articleId, Model model) {
        Article article = articleRepository.findById(articleId).orElse(null);
        WriteArticleForm writeArticleForm = WriteArticleForm.builder()
                .board(article.getBoard())
                .title(article.getTitle())
                .content(article.getContent())
                .user(article.getUser())
                .viewCount(article.getViewCount())
                .build();
        model.addAttribute("article", writeArticleForm);
        return "editArticleForm";

    }

    @PostMapping("/articles/{articleId}/edit")
    public String edit(@PathVariable Long articleId, @Validated @ModelAttribute("article") WriteArticleForm writeArticleForm, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            log.info("errors={}", bindingResult);
            return "editArticleForm";
        }
        articleService.editArticle(articleId, writeArticleForm.getBoard(), writeArticleForm.getTitle(), writeArticleForm.getContent());
        return "redirect:/articles/{articleId}";
    }

    @ResponseBody
    @PostMapping("/images/article")
    public Map<String, Object> image(@RequestParam Map<String, Object> paramMap, MultipartHttpServletRequest request) throws Exception {


        // ckeditor 에서 파일을 보낼 때 upload : [파일] 형식으로 해서 넘어오기 때문에 upload라는 키의 밸류를 받아서 uploadFile에 저장함
        MultipartFile uploadFile = request.getFile("upload");

        // 파일의 오리지널 네임
        String originalFileName = uploadFile.getOriginalFilename();

        // 파일의 확장자
        String ext = originalFileName.substring(originalFileName.indexOf("."));

        // 서버에 저장될 때 중복된 파일 이름인 경우를 방지하기 위해 UUID에 확장자를 붙여 새로운 파일 이름을 생성
        String newFileName = UUID.randomUUID() + ext;

        // 현재경로/upload/파일명이 저장 경로
        String savePath = fileStore.getArticleImagePath(newFileName);

        // 브라우저에서 이미지 불러올 때 절대 경로로 불러오면 보안의 위험 있어 상대경로를 쓰거나 이미지 불러오는 jsp 또는 클래스 파일을 만들어 가져오는 식으로 우회해야 함
        // 때문에 savePath와 별개로 상대 경로인 uploadPath 만들어줌
        String uploadPath = "/images/article/" + newFileName;

        // 저장 경로로 파일 객체 생성
        File file = new File(savePath);

        // 파일 업로드
        uploadFile.transferTo(file);

        paramMap.put("url", uploadPath);

        return paramMap;
    }

    @PostMapping("/like/{articleId}")
    public String like(@PathVariable Long articleId, @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser){

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
        for (int i = 0; i < 400; i++) {
            articleRepository.save(new Article("보디빌딩", "오운완" + i, "오늘도 운동 완료", user, 0, 0));
        }
//        articleRepository.save(new Article("보디빌딩", "오운완", "오늘도 운동 완료", user));
//        articleRepository.save(new Article("파워리프팅", "3대 500달성", "힘들다", user));
//        articleRepository.save(new Article("파워리프팅", "3대 500달성", "힘들다", user));

    }
}
