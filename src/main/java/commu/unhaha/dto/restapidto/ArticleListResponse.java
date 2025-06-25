package commu.unhaha.dto.restapidto;

import commu.unhaha.dto.ArticlesDto;
import commu.unhaha.dto.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.domain.Page;



@Schema(description = "게시글 리스트 응답")
@Data
public class ArticleListResponse {
    @Schema(description = "게시글 페이지")
    private Page<ArticlesDto> articles;

    @Schema(description = "게시글 페이지 정보")
    private PageInfo pageInfo;

    @Schema(description = "게시판 타입")
    private String boardType;

    @Schema(description = "게시판 타입")
    private String type;

    @Schema(description = "게시판 이름")
    private String title;

    @Schema(description = "검색 타입")
    private String searchType;

    @Schema(description = "검색 키워드")
    private String keyword;

}
