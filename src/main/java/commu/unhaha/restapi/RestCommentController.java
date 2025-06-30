package commu.unhaha.restapi;

import commu.unhaha.controller.SessionConst;
import commu.unhaha.domain.Comment;
import commu.unhaha.domain.User;
import commu.unhaha.dto.CommentDto;
import commu.unhaha.dto.CreateCommentRequest;
import commu.unhaha.dto.SessionUser;
import commu.unhaha.repository.UserRepository;
import commu.unhaha.service.CommentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api")
@Tag(name = "댓글 API", description = "댓글 관련 CRUD API")
public class RestCommentController implements CommentApiDocs {
    private final CommentService commentService;
    private final UserRepository userRepository;

    private boolean isValidBoardType(String boardType) {
        return Arrays.asList("new", "best", "bodybuilding", "powerlifting", "crossfit").contains(boardType);
    }

    /**
     * 댓글 작성 API
     */
    @PostMapping("/{boardType}/{articleId}/comments")
    public ResponseEntity<Map<String, Object>> createComment(
            @PathVariable String boardType,
            @PathVariable Long articleId,
            @Valid @RequestBody CreateCommentRequest request,
            @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser) {

        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인이 필요합니다."));
        }
        // 유효한 boardType인지 검증
        if (!isValidBoardType(boardType)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "잘못된 게시판 타입입니다: " + boardType));
        }

        try {
            User user = userRepository.findByEmail(loginUser.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            Comment savedComment = commentService.createComment(
                    user, articleId, request.getContent(), request.getImageUrl(), request.getParentId());
            CommentDto commentDto = new CommentDto(savedComment);
            // 작성된 댓글이 있는 페이지 계산
            int targetPage = commentService.calculateCommentRedirectPage(savedComment, articleId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("commentId", commentDto.getId());
            response.put("parentId", commentDto.getParentId());
            response.put("targetPage", targetPage);
            response.put("message", "댓글이 성공적으로 작성되었습니다.");
            response.put("redirectUrl", String.format("/%s/%d?page=%d#comment%d",
                    boardType, articleId, targetPage, commentDto.getId()));

            log.info("댓글 작성 완료 - API 응답: prefix={}, page={}, commentId={}",
                    boardType, targetPage, savedComment.getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("댓글 작성 중 오류 발생: articleId={}", articleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "댓글 작성 중 오류가 발생했습니다."));
        }
    }

    /**
     * 댓글 수정 API
     */
    @PutMapping("/{boardType}/{articleId}/comments/{commentId}")
    public ResponseEntity<Map<String, Object>> editComment(
            @PathVariable String boardType,
            @PathVariable Long commentId,
            @Valid @RequestBody CreateCommentRequest request,
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
            commentService.updateComment(commentId, request.getContent(), request.getImageUrl(), loginUser.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "댓글이 성공적으로 수정되었습니다.");
            response.put("commentId", commentId);

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {  // SecurityException 먼저 처리
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("댓글 수정 중 오류 발생: commentId={}", commentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "댓글 수정 중 오류가 발생했습니다."));
        }
    }

    /**
     * 댓글 삭제 API
     */
    @DeleteMapping("/{boardType}/{articleId}/comments/{commentId}")
    public ResponseEntity<Map<String, Object>> deleteComment(
            @PathVariable String boardType,
            @PathVariable Long commentId,
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
            commentService.deleteComment(commentId, loginUser.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "댓글이 성공적으로 삭제되었습니다.");
            response.put("redirectUrl", String.format("/%s/%d", boardType, articleId));

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {  // SecurityException 먼저 처리
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("댓글 삭제 중 오류 발생: commentId={}", commentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "댓글 삭제 중 오류가 발생했습니다."));
        }
    }

    /**
     * 댓글 좋아요 API
     */
    @PostMapping("/{boardType}/{articleId}/comments/{commentId}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @PathVariable String boardType,
            @PathVariable Long commentId,
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
            boolean liked = commentService.saveLike(commentId, loginUser.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("liked", liked);
            response.put("message", liked ? "좋아요가 추가되었습니다." : "좋아요가 취소되었습니다.");
            response.put("commentId", commentId);

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {  // SecurityException 먼저 처리
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("댓글 좋아요 처리 중 오류 발생: commentId={}", commentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "좋아요 처리 중 오류가 발생했습니다."));
        }
    }
}
