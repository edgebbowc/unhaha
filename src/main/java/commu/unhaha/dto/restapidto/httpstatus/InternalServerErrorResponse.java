package commu.unhaha.dto.restapidto.httpstatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "서버에서 오류가 발생했습니다.")
@Data
public class InternalServerErrorResponse {

    @Schema(description = "에러 메시지", example = "내부 서버 오류 발생")
    private String error;

}
