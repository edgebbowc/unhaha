package commu.unhaha.dto.restapidto.httpstatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "댓글을 찾을 수 없습니다")
@Data
public class CommentNotFoundResponse {
    @Schema(description = "에러 메시지", example = "댓글을 찾을 수 없습니다.")
    private String error;
}
