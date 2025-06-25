package commu.unhaha.dto.restapidto;

import commu.unhaha.dto.ArticlesDto;
import commu.unhaha.dto.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.domain.Page;

@Schema(description = "홈페이지 응답")
@Data
public class HomeResponse {
    @Schema(description = "게시글 목록")
    private Page<ArticlesDto> articles;

    @Schema(description = "페이지 정보")
    private PageInfo pageInfo;

    @Schema(description = "검색 타입")
    private String searchType;

    @Schema(description = "검색 키워드")
    private String keyword;
}
