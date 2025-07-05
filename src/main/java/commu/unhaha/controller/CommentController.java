package commu.unhaha.controller;

import commu.unhaha.domain.Comment;
import commu.unhaha.domain.CommentImage;
import commu.unhaha.domain.UploadFile;
import commu.unhaha.domain.User;
import commu.unhaha.dto.SessionUser;
import commu.unhaha.file.GCSFileStore;
import commu.unhaha.repository.CommentImageRepository;
import commu.unhaha.repository.UserRepository;
import commu.unhaha.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


@Controller
@RequiredArgsConstructor
@Slf4j
public class CommentController {
    private final CommentService commentService;
    private final UserRepository userRepository;
    private final GCSFileStore gcsFileStore;
    private final CommentImageRepository commentImageRepository;

    private boolean isValidBoardType(String boardType) {
        return Arrays.asList("new", "best", "bodybuilding", "powerlifting", "crossfit", "humor").contains(boardType);
    }
    /**
     * 댓글 작성
     */
    @PostMapping("/{boardType}/{articleId}/comments")
    public String createComment(@PathVariable String boardType,
                                @PathVariable Long articleId,
                                @RequestParam String content,
                                @RequestParam(required = false) List<String> imageUrl,
                                @RequestParam(required = false) Long parentId,
                                @RequestParam(required = false, defaultValue = "1") Integer currentListPage,
                                @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser) {

        // 유효한 boardType인지 검증
        if (!isValidBoardType(boardType)) {
            throw new IllegalArgumentException("잘못된 게시판 타입입니다: " + boardType);
        }

        if (loginUser == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByEmail(loginUser.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 단일 메서드로 통합 - 이미지 유무와 관계없이 동일한 메서드 사용
        Comment savedComment = commentService.createComment(user, articleId, content, imageUrl, parentId);

        // 작성된 댓글이 있는 페이지 계산
        int currentCommentPage = commentService.calculateCommentRedirectPage(savedComment, articleId);

        // UriComponentsBuilder 사용
        String redirectUrl = UriComponentsBuilder
                .fromPath("/{boardType}/{articleId}")
                .queryParam("page", currentListPage)
                .queryParam("comment", currentCommentPage)
                .fragment("comment" + savedComment.getId())
                .buildAndExpand(boardType, articleId)
                .toUriString(); // toString() 대신 toUriString() 사용
        log.info("댓글 작성 완료 - 리다이렉트: boardType= {}, listPage={}, page= {}, commentId= {}", boardType, currentListPage, currentCommentPage, savedComment.getId());
        return "redirect:" + redirectUrl;
    }


    /**
     * 댓글 이미지 전송
     */
    @ResponseBody
    @PostMapping("/comments/images")
    public ResponseEntity<Map<String, Object>> uploadCommentImage(@RequestParam("file") MultipartFile file,
                                                                  @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser){
        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인이 필요합니다."));
        }
        try {
            // 파일 유효성 검사
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "파일이 비어있습니다."));
            }

            // 이미지 파일인지 확인
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "이미지 파일만 업로드 가능합니다."));
            }

            // 파일 크기 제한 (예: 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("error", "파일 크기는 5MB를 초과할 수 없습니다."));
            }

            // GCS에 업로드
            UploadFile uploadFile = gcsFileStore.storeCommentImage(file);

            //CommentImage에 임시파일로 저장
            CommentImage commentImage = CommentImage.createTemp(uploadFile.getStoreFileUrl());
            commentImageRepository.save(commentImage);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "imageUrl", uploadFile.getStoreFileUrl()
            ));

        } catch (Exception e) {
            log.error("이미지 업로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "이미지 업로드에 실패했습니다."));
        }
    }

    /**
     * 댓글 수정
     */
    @PostMapping("/{boardType}/{articleId}/comments/{commentId}")
    public String editComment(@PathVariable String boardType,
                              @PathVariable Long articleId,
                              @PathVariable Long commentId,
                              @RequestParam String content,
                              @RequestParam(required = false) List<String> imageUrl,
                              @RequestParam(value = "currentListPage") Integer currentListPage,
                              @RequestParam(value = "currentCommentPage") Integer currentCommentPage,
                              @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser) {
        // 유효한 boardType인지 검증
        if (!isValidBoardType(boardType)) {
            throw new IllegalArgumentException("잘못된 게시판 타입입니다: " + boardType);
        }

        if (loginUser == null) {
            return "redirect:/login";
        }

        try {
            // 댓글 수정
            commentService.updateComment(commentId, content, imageUrl, loginUser.getEmail());

            // 리다이렉트 URL 생성
            String redirectUrl = UriComponentsBuilder
                    .fromPath("/{boardType}/{articleId}")
                    .queryParam("page", currentListPage)
                    .queryParam("comment", currentCommentPage)
                    .fragment("comment" + commentId)
                    .buildAndExpand(boardType, articleId)
                    .toUriString();

            return "redirect:" + redirectUrl;

        } catch (Exception e) {
            log.error("댓글 수정 중 오류 발생: commentId={}", commentId, e);
            return "redirect:/" + boardType + "/"+ articleId;
        }
    }

    /**
     * 댓글 삭제
     */
    @DeleteMapping("/{boardType}/{articleId}/comments/{commentId}")
    public String deleteComment(@PathVariable String boardType,
                                @PathVariable Long commentId,
                                @PathVariable Long articleId,
                                @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser) {
        // 유효한 boardType인지 검증
        if (!isValidBoardType(boardType)) {
            throw new IllegalArgumentException("잘못된 게시판 타입입니다: " + boardType);
        }

        if (loginUser == null) {
            return "redirect:/login";
        }

        try {
            commentService.deleteComment(commentId, loginUser.getEmail());
            return "redirect:/" + boardType + "/" + articleId;
        } catch (Exception e) {
            log.error("댓글 삭제 중 오류 발생: commentId={}", commentId, e);
            return "redirect:/" + boardType + "/" + articleId;
        }
    }

    /**
     * 댓글 좋아요
     */
    @PostMapping("/{boardType}/{articleId}/comments/{commentId}/like")
    public String toggleLike(@PathVariable String boardType,
                             @PathVariable Long commentId,
                             @PathVariable Long articleId,
                             @RequestParam(value = "currentCommentPage") Integer currentPage,
                             @SessionAttribute(name = SessionConst.LOGIN_USER) SessionUser loginUser) {
        // 유효한 boardType인지 검증
        if (!isValidBoardType(boardType)) {
            throw new IllegalArgumentException("잘못된 게시판 타입입니다: " + boardType);
        }

        try {
            Long loginUserId = userRepository.findByEmail(loginUser.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다.")).getId();

            commentService.saveLike(commentId, loginUserId);

            // 현재 페이지로 리다이렉트
            String redirectUrl = UriComponentsBuilder
                    .fromPath("/{boardType}/{articleId}")
                    .queryParam("page", currentPage)
                    .fragment("comment" + commentId) // 해당 댓글로 스크롤
                    .buildAndExpand(boardType, articleId)
                    .toUriString();

            log.info("댓글 좋아요 처리 완료 - 리다이렉트: page={}, commentId={}", currentPage, commentId);
            return "redirect:" + redirectUrl;

        } catch (Exception e) {
            log.error("댓글 좋아요 처리 중 오류 발생: commentId={}", commentId, e);
            return "redirect:" + boardType + "/" + articleId + "?page=" + currentPage;
        }
    }
}
