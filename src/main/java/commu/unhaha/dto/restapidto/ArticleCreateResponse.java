package commu.unhaha.dto.restapidto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "게시글 생성 응답")
@Data
public class ArticleCreateResponse {
    @Schema(description = "성공 여부", example = "true")
    private boolean success;

    @Schema(description = "생성된 게시글 ID", example = "123")
    private Long articleId;

    @Schema(description = "응답 메시지", example = "게시글이 성공적으로 작성되었습니다.")
    private String message;
}
