package commu.unhaha.dto.restapidto.httpstatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "로그인 필요 응답")
@Data
public class RequiredLoginResponse {
    @Schema(description = "에러 메시지", example = "로그인이 필요합니다.")
    private String error;
}
