package commu.unhaha.dto.restapidto;

import commu.unhaha.dto.ArticleDto;
import commu.unhaha.dto.CommentDto;
import commu.unhaha.dto.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "게시글 상세 응답")
@Data
public class ArticleDetailResponse {
    @Schema(description = "게시글 정보")
    private ArticleDto article;

    @Schema(description = "댓글 목록")
    private List<CommentDto> comments;

    @Schema(description = "댓글 페이지 정보")
    private PageInfo commentPageInfo;

    @Schema(description = "네비게이션 정보")
    private Map<String, Object> navigation;

    @Schema(description = "게시판 타입")
    private String boardType;
}
