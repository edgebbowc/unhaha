package commu.unhaha.controller;

import commu.unhaha.domain.User;
import commu.unhaha.dto.SessionUser;
import commu.unhaha.repository.UserRepository;
import commu.unhaha.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;


@Controller
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    private final UserRepository userRepository;

    // 댓글 작성
    @PostMapping("/articles/{articleId}/comments")
    public String createComment(@PathVariable Long articleId,
                                @RequestParam String content,
                                @RequestParam(required = false) Long parentId,
                                @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser) {

        if (loginUser == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByEmail(loginUser.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        commentService.createComment(user, articleId, content, parentId);

        return "redirect:/articles/" + articleId;
    }

    // 댓글 수정
    @PostMapping("/{commentId}/edit")
    public String editComment(@PathVariable Long commentId,
                              @RequestParam String content,
                              @RequestParam Long articleId) {
        commentService.updateComment(commentId, content);
        return "redirect:/articles/" + articleId;
    }

    // 댓글 삭제
    @PostMapping("/{commentId}/delete")
    public String deleteComment(@PathVariable Long commentId,
                                @RequestParam Long articleId) {
        commentService.deleteComment(commentId);
        return "redirect:/articles/" + articleId;
    }

    // 댓글 좋아요
    @PostMapping("/{commentId}/like")
    public String toggleLike(@PathVariable Long commentId,
                             @RequestParam Long articleId,
                             @SessionAttribute(name = SessionConst.LOGIN_USER) SessionUser loginUser) {
        Long loginUserId = userRepository.findByEmail(loginUser.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다.")).getId();
        commentService.saveLike(commentId, loginUserId);
        return "redirect:/articles/" + articleId;
    }
}
