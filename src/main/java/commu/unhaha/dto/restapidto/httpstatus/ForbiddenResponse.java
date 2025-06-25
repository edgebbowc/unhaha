package commu.unhaha.dto.restapidto.httpstatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "권한 없음")
@Data
public class ForbiddenResponse {
    @Schema(description = "에러 메시지", example = "권한이 없습니다.")
    private String error;
}
