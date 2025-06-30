package commu.unhaha.restapi;

import commu.unhaha.controller.SessionConst;
import commu.unhaha.dto.CreateCommentRequest;
import commu.unhaha.dto.SessionUser;
import commu.unhaha.dto.restapidto.*;
import commu.unhaha.dto.restapidto.httpstatus.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

public interface CommentApiDocs {

    /**
     * 댓글 작성 API
     */
    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "댓글 작성", description = "새로운 댓글을 작성합니다. 로그인이 필요합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "댓글 작성 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CommentCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 게시판 타입",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WrongBoardTypeResponse.class))),
            @ApiResponse(responseCode = "401", description = "로그인 필요",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RequiredLoginResponse.class))),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CommentNotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PostMapping("/{boardType}/{articleId}/comments")
    ResponseEntity<Map<String, Object>> createComment(
            @Parameter(description = "게시판 타입", example = "bodybuilding",
                    schema = @Schema(allowableValues = {"new", "best", "bodybuilding", "powerlifting", "crossfit", "wrong-board"}))
            @PathVariable String boardType,

            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long articleId,

            @Parameter(description = "댓글 작성 내용", required = true)
            @Valid @RequestBody CreateCommentRequest request,

            @Parameter(hidden = true)
            @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser);

    /**
     * 댓글 수정 API
     */
    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "댓글 수정", description = "기존 댓글을 수정합니다. 본인이 작성한 댓글만 수정 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "댓글 수정 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = "{\"success\": true, \"message\": \"댓글이 성공적으로 수정되었습니다.\", \"commentId\": 1}"))),
            @ApiResponse(responseCode = "400", description = "잘못된 게시판 타입",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WrongBoardTypeResponse.class))),
            @ApiResponse(responseCode = "401", description = "로그인 필요",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RequiredLoginResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음 (본인 댓글이 아님)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ForbiddenResponse.class))),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CommentNotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PutMapping("/{boardType}/{articleId}/comments/{commentId}")
    ResponseEntity<Map<String, Object>> editComment(
            @Parameter(description = "게시판 타입", example = "bodybuilding",
                    schema = @Schema(allowableValues = {"new", "best", "bodybuilding", "powerlifting", "crossfit", "wrong-board"}))
            @PathVariable String boardType,

            @Parameter(description = "댓글 ID", example = "1")
            @PathVariable Long commentId,

            @Parameter(description = "댓글 수정 내용", required = true)
            @Valid @RequestBody CreateCommentRequest request,

            @Parameter(hidden = true)
            @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser);

    /**
     * 댓글 삭제 API
     */
    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다. 본인이 작성한 댓글만 삭제 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "댓글 삭제 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = "{\"success\": true, \"message\": \"댓글이 성공적으로 삭제되었습니다.\", \"redirectUrl\": \"/bodybuilding/1\"}"))),
            @ApiResponse(responseCode = "400", description = "잘못된 게시판 타입",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WrongBoardTypeResponse.class))),
            @ApiResponse(responseCode = "401", description = "로그인 필요",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RequiredLoginResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음 (본인 댓글이 아님)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ForbiddenResponse.class))),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CommentNotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @DeleteMapping("/{boardType}/{articleId}/comments/{commentId}")
    ResponseEntity<Map<String, Object>> deleteComment(
            @Parameter(description = "게시판 타입", example = "bodybuilding",
                    schema = @Schema(allowableValues = {"new", "best", "bodybuilding", "powerlifting", "crossfit", "wrong-board"}))
            @PathVariable String boardType,

            @Parameter(description = "댓글 ID", example = "1")
            @PathVariable Long commentId,

            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long articleId,

            @Parameter(hidden = true)
            @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser);

    /**
     * 댓글 좋아요 API
     */
    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "댓글 좋아요/취소", description = "댓글에 좋아요를 추가하거나 취소합니다. 토글 방식으로 동작합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "좋아요 처리 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = "{\"success\": true, \"liked\": true, \"message\": \"좋아요가 추가되었습니다.\", \"commentId\": 1}"))),
            @ApiResponse(responseCode = "400", description = "잘못된 게시판 타입",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WrongBoardTypeResponse.class))),
            @ApiResponse(responseCode = "401", description = "로그인 필요",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RequiredLoginResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음 (본인 댓글이 아님)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ForbiddenResponse.class))),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CommentNotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PostMapping("/{boardType}/{articleId}/comments/{commentId}/like")
    ResponseEntity<Map<String, Object>> toggleLike(
            @Parameter(description = "게시판 타입", example = "bodybuilding",
                    schema = @Schema(allowableValues = {"new", "best", "bodybuilding", "powerlifting", "crossfit", "wrong-board"}))
            @PathVariable String boardType,

            @Parameter(description = "댓글 ID", example = "1")
            @PathVariable Long commentId,

            @Parameter(hidden = true)
            @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser);
}
