package commu.unhaha.dto.restapidto.httpstatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "게시글을 찾을 수 없습니다")
@Data
public class ArticleNotFoundResponse {
    @Schema(description = "에러 메시지", example = "해당 게시글을 찾을 수 없습니다.")
    private String error;
}
