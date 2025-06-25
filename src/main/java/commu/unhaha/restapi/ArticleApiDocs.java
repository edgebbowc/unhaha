package commu.unhaha.restapi;

import commu.unhaha.controller.SessionConst;
import commu.unhaha.dto.SessionUser;
import commu.unhaha.dto.WriteArticleForm;
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

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;

public interface ArticleApiDocs {

    /**
     * 홈페이지 (인기글 목록) API
     */
    @Operation(summary = "인기글 목록 조회", description = "홈페이지에 표시될 인기글 목록을 페이지네이션과 검색 기능으로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인기글 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = HomeResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @GetMapping("/home")
    ResponseEntity<HomeResponse> home(
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(value = "page", defaultValue = "1") int page,

            @Parameter(description = "검색 타입", schema = @Schema(allowableValues = {"title", "titleandcontent", "nickname"}))
            @RequestParam(value = "searchType", required = false) String searchType,

            @Parameter(description = "검색 키워드")
            @RequestParam(value = "keyword", required = false) String keyword
    );

    /**
     * 게시글 생성 API
     */
    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "게시글 작성", description = "새로운 게시글을 작성합니다. 로그인이 필요합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "게시글 작성 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ArticleCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 게시판 타입",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WrongBoardTypeResponse.class))),
            @ApiResponse(responseCode = "401", description = "로그인 필요",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RequiredLoginResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PostMapping("/articles/{boardType}")
    ResponseEntity<Map<String, Object>> createArticle(
            @Parameter(description = "게시판 타입", example = "bodybuilding",
                    schema = @Schema(allowableValues = {"new", "bodybuilding", "powerlifting", "crossfit", "wrong-board"}))
            @PathVariable String boardType,

            @Parameter(description = "게시글 작성 내용", required = true)
            @Valid @RequestBody WriteArticleForm form,

            @Parameter(hidden = true) // Swagger에서 숨김
            @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser);

    /**
     * 게시글 수정 API
     */
    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "게시글 수정", description = "기존 게시글을 수정합니다. 작성자만 수정 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "게시글 수정 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = "{\"success\": true, \"articleId\": 1, \"message\": \"게시글이 성공적으로 수정되었습니다.\"}"))),
            @ApiResponse(responseCode = "400", description = "잘못된 게시판 타입",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WrongBoardTypeResponse.class))),
            @ApiResponse(responseCode = "401", description = "로그인 필요",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RequiredLoginResponse.class))),
            @ApiResponse(responseCode = "403", description = "수정 권한 없음 (작성자가 아님)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ForbiddenResponse.class))),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ArticleNotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PutMapping("/articles/{boardType}/{articleId}")
    ResponseEntity<Map<String, Object>> updateArticle(
            @Parameter(description = "게시판 타입", example = "bodybuilding",
                    schema = @Schema(allowableValues = {"new", "best", "bodybuilding", "powerlifting", "crossfit", "wrong-board"}))
            @PathVariable String boardType,

            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long articleId,

            @Parameter(description = "수정할 게시글 내용", required = true)
            @Valid @RequestBody WriteArticleForm form,

            @Parameter(hidden = true)
            @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser);

    /**
     * 게시글 삭제 API
     */
    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "게시글 삭제", description = "게시글을 삭제합니다. 작성자만 삭제 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "게시글 삭제 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = "{\"success\": true, \"articleId\": 1, \"message\": \"댓글이 성공적으로 삭제되었습니다.\"}"))),
            @ApiResponse(responseCode = "400", description = "잘못된 게시판 타입",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WrongBoardTypeResponse.class))),
            @ApiResponse(responseCode = "401", description = "로그인 필요",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RequiredLoginResponse.class))),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음 (작성자가 아님)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ForbiddenResponse.class))),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ArticleNotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @DeleteMapping("/articles/{boardType}/{articleId}")
    ResponseEntity<Map<String, Object>> deleteArticle(
            @Parameter(description = "게시판 타입", example = "bodybuilding",
                    schema = @Schema(allowableValues = {"new", "best", "bodybuilding", "powerlifting", "crossfit", "wrong-board"}))
            @PathVariable String boardType,

            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long articleId,

            @Parameter(hidden = true)
            @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser);

    /**
     * 게시판 목록 API
     */
    @Operation(summary = "게시판별 게시글 목록 조회", description = "특정 게시판의 게시글 목록을 페이지네이션과 검색 기능으로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "게시글 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ArticleListResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 게시판 타입",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WrongBoardTypeResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @GetMapping("/articles/{boardType}")
    ResponseEntity<Map<String, Object>> getArticleList(
            @Parameter(description = "게시판 타입", example = "bodybuilding",
                    schema = @Schema(allowableValues = {"new", "best", "bodybuilding", "powerlifting", "crossfit", "wrong-board"}))
            @PathVariable String boardType,

            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(value = "page", defaultValue = "1") int page,

            @Parameter(description = "검색 타입", schema = @Schema(allowableValues = {"title", "content", "author"}))
            @RequestParam(value = "searchType", required = false) String searchType,

            @Parameter(description = "검색 키워드")
            @RequestParam(value = "keyword", required = false) String keyword);

    /**
     * 게시글 상세 조회 API
     */
    @Operation(summary = "게시글 상세 조회", description = "특정 게시글의 상세 정보와 댓글을 조회합니다. 조회수가 증가합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "게시글 상세 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ArticleDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 게시판 타입",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WrongBoardTypeResponse.class))),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ArticleNotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @GetMapping("/articles/{boardType}/{articleId}")
    ResponseEntity<Map<String, Object>> getArticleDetail(
            @Parameter(description = "게시판 타입", example = "bodybuilding",
                    schema = @Schema(allowableValues = {"new", "best", "bodybuilding", "powerlifting", "crossfit", "wrong-board"}))
            @PathVariable String boardType,

            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long articleId,

            @Parameter(description = "댓글 페이지 번호", example = "1")
            @RequestParam(value = "page", required = false) Integer page,

            @Parameter(hidden = true)
            @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser,

            @Parameter(hidden = true)
                    HttpServletRequest request);

    /**
     * 게시글 좋아요 API
     */
    @SecurityRequirement(name = "cookieAuth")
    @Operation(summary = "게시글 좋아요/취소", description = "게시글에 좋아요를 추가하거나 취소합니다. 자신의 게시글에는 좋아요를 할 수 없습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "좋아요 처리 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LikeResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 게시판 타입",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WrongBoardTypeResponse.class))),
            @ApiResponse(responseCode = "401", description = "로그인 필요",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RequiredLoginResponse.class))),
            @ApiResponse(responseCode = "403", description = "자신의 게시글에는 좋아요 불가",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ForbiddenResponse.class))),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ArticleNotFoundResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InternalServerErrorResponse.class)))
    })
    @PostMapping("/articles/{boardType}/{articleId}/like")
    ResponseEntity<Map<String, Object>> toggleLike(
            @Parameter(description = "게시판 타입", example = "bodybuilding",
                    schema = @Schema(allowableValues = {"new", "best", "bodybuilding", "powerlifting", "crossfit", "wrong-board"}))
            @PathVariable String boardType,

            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long articleId,

            @Parameter(hidden = true)
            @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser);
}
