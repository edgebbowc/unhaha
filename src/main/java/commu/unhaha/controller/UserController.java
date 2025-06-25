package commu.unhaha.controller;

import commu.unhaha.domain.UploadFile;
import commu.unhaha.dto.*;
import commu.unhaha.file.GCSFileStore;
import commu.unhaha.service.ArticleService;
import commu.unhaha.service.CommentService;
import commu.unhaha.service.UserService;
import commu.unhaha.util.TimeAgoFormatter;
import commu.unhaha.validation.NicknameValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final NicknameValidator nicknameValidator;
    private final GCSFileStore gcsFileStore;
    private final ArticleService articleService;
    private final CommentService commentService;

    @GetMapping("/mypage")
    public String Profile(@SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser, Model model) {

        // 로그인 상태 확인
        if (loginUser == null) {
            return "redirect:/login"; // 로그인 페이지로 리다이렉트
        }
        model.addAttribute("user", loginUser);

        return "mypage";
    }

    @PostMapping("/mypage")
    public String editProfile(@Validated @ModelAttribute("user") MypageForm mypageForm, BindingResult bindingResult,
                              @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser,
                              HttpSession session, Model model, RedirectAttributes rttr) throws IOException {
        // Bean Validation 적용
        if (bindingResult.hasErrors()) {
            log.info("errors={}", bindingResult);
            mypageForm.setStoredImageName(loginUser.getStoredImageName());
            model.addAttribute("msg", "3글자 이상 입력해주세요 특수문자 및 공백은 불가능합니다");
            return "mypage";
        }
        // 원래 닉네임과 같을 경우
        if ((mypageForm.getNickname().equals(loginUser.getNickname()))) {
            rttr.addFlashAttribute("msg", "회원정보를 변경하였습니다");
            return "redirect:/mypage";
        }

        // 닉네임 중복 검증
        nicknameValidator.validate(mypageForm, bindingResult);
        if (bindingResult.hasErrors()) {
            mypageForm.setStoredImageName(loginUser.getStoredImageName());
            model.addAttribute("msg", "이미 사용중인 닉네임입니다");
            return "mypage";
        }

        // 프로필이미지 없는상태에서 닉네임 변경
        if (mypageForm.getUserImage().isEmpty()) {
            userService.editNickname(mypageForm.getEmail(), mypageForm.getNickname());
            mypageForm.setStoredImageName(loginUser.getStoredImageName());
            setSession(mypageForm, loginUser, session);
            rttr.addFlashAttribute("msg", "회원정보를 변경하였습니다");
            return "redirect:/mypage";
        }

        // 프로필이미지 있는상태에서 닉네임 변경
        userService.editNickname(mypageForm.getEmail(), mypageForm.getNickname());
        setSession(mypageForm, loginUser, session);
        rttr.addFlashAttribute("msg", "회원정보를 변경하였습니다");
        return "redirect:/mypage";
    }

    // 프로필 이미지 저장
    @PostMapping("/mypage/upload-profile-image")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadProfileImage(
            @RequestParam("image") MultipartFile image,
            @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser,
            HttpSession session) throws IOException {

        if (image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false));
        }

        gcsFileStore.deleteFile(loginUser.getStoredImageName()); // 기존 이미지 삭제
        UploadFile uploadFile = gcsFileStore.storeProfileImage(image);   // GCS에 새 이미지 업로드

        // DB 반영
        userService.editProfileImage(loginUser.getEmail(), uploadFile);

        // 세션 업데이트
        loginUser.setStoredImageName(uploadFile.getStoreFileUrl());
        session.setAttribute(SessionConst.LOGIN_USER, loginUser);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "imageUrl", uploadFile.getStoreFileUrl()
        ));
    }

    @GetMapping("/mypage/withdraw")
    public String withDraw(@SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser,
                           RedirectAttributes rttr, HttpSession session){
        try {
            userService.deleteUser(loginUser.getEmail());
            // SecurityContext 먼저 정리
            SecurityContextHolder.clearContext();
            session.invalidate();
            rttr.addFlashAttribute("result", "회원 탈퇴에 성공했습니다");
        } catch (Exception e) {
            log.error("회원탈퇴 처리 중 오류 발생", e);
            rttr.addFlashAttribute("error", "회원 탈퇴에 실패했습니다");
        }

        return "redirect:/";
    }

    @GetMapping("/mypage/article")
    public String myArticle(Model model,
                            @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser) {
        // 로그인 상태 확인
        if (loginUser == null) {
            return "redirect:/login"; // 로그인 페이지로 리다이렉트
        }
        // 사용자가 작성한 게시글 목록 조회
        List<ArticleDto> articles = articleService.findArticlesByUserEmail(loginUser.getEmail());
        setArticleDatetime(articles);

        // 모델에 게시글 목록 추가
        model.addAttribute("articles", articles);

        return "mypageArticle";
    }

    @GetMapping("/mypage/article/like")
    public String myArticleLike(Model model,
                            @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser) {
        // 로그인 상태 확인
        if (loginUser == null) {
            return "redirect:/login"; // 로그인 페이지로 리다이렉트
        }
        // 사용자가 좋아요한 게시글 목록 조회
        List<ArticleDto> likedArticles = articleService.findLikedArticlesByUserEmail(loginUser.getEmail());
        setArticleDatetime(likedArticles);

        // 모델에 게시글 목록 추가
        model.addAttribute("articles", likedArticles);

        return "mypageArticleLike";
    }

    @GetMapping("/mypage/comment")
    public String myComment(Model model,
                            @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser) {
        // 로그인 상태 확인
        if (loginUser == null) {
            return "redirect:/login"; // 로그인 페이지로 리다이렉트
        }
        // 사용자가 작성한 댓글 목록 조회
        List<CommentDto> comments = commentService.findCommentsByUserEmail(loginUser.getEmail());
        for (CommentDto comment : comments) {
            setCommentDatetime(comment);
            setCommentImageUrls(comment);
        }

        // 모델에 게시글 목록 추가
        model.addAttribute("comments", comments);

        return "mypageComment";
    }

    @GetMapping("/mypage/comment/like")
    public String myCommentLike(Model model,
                            @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser) {
        // 로그인 상태 확인
        if (loginUser == null) {
            return "redirect:/login"; // 로그인 페이지로 리다이렉트
        }

        // 사용자가 좋아요한 댓글 목록 조회
        List<CommentDto> likedComments = commentService.findLikedCommentsByUserEmail(loginUser.getEmail());
        for (CommentDto comment : likedComments) {
            setCommentDatetime(comment);
            setCommentImageUrls(comment);
        }

        // 모델에 게시글 목록 추가
        model.addAttribute("comments", likedComments);

        return "mypageCommentLike";
    }

    private void setSession(MypageForm mypageForm, SessionUser loginUser, HttpSession session) {
        SessionUser sessionUser = new SessionUser(mypageForm, loginUser);
        session.setAttribute(SessionConst.LOGIN_USER, sessionUser);
    }

    private void setArticleDatetime(List<ArticleDto> articles) {
        for (ArticleDto articleDto : articles) {
            String dateTime = TimeAgoFormatter.format((articleDto.getCreatedDate()), LocalDateTime.now());
            articleDto.setDateTime(dateTime);
        }
    }

    private void setCommentDatetime(CommentDto commentDto) {
            String dateTime = TimeAgoFormatter.format((commentDto.getCreatedDate()), LocalDateTime.now());
            commentDto.setDateTime(dateTime);
    }

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

}
