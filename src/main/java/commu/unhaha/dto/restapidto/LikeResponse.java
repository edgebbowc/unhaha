package commu.unhaha.dto.restapidto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "좋아요 응답")
@Data
public class LikeResponse {
    @Schema(description = "성공 여부", example = "true")
    private boolean success;

    @Schema(description = "좋아요 상태", example = "true")
    private boolean liked;

    @Schema(description = "게시글 ID", example = "123")
    private Long articleId;

    @Schema(description = "응답 메시지", example = "좋아요가 추가되었습니다.")
    private String message;
}
