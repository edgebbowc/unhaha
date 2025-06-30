package commu.unhaha.dto.restapidto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "댓글 작성 응답")
@Data
public class CommentCreateResponse {
    @Schema(description = "성공 여부", example = "true")
    private boolean success;

    @Schema(description = "댓글 ID", example = "1")
    private Long commentId;

    @Schema(description = "부모 댓글 ID", example = "null")
    private Long parentId;

    @Schema(description = "리다이렉트할 페이지", example = "1")
    private int targetPage;

    @Schema(description = "응답 메시지", example = "댓글이 성공적으로 작성되었습니다.")
    private String message;

    @Schema(description = "리다이렉트 URL", example = "/bodybuilding/1?page=1#comment1")
    private String redirectUrl;
}
