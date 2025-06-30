package commu.unhaha.dto.restapidto.httpstatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "잘못된 게시판 타입 응답")
@Data
public class WrongBoardTypeResponse {
    @Schema(description = "에러 메시지", example = "Invalid board type: 잘못된 게시판 타입")
    private String error;
}
