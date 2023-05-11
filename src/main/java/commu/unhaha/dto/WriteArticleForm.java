package commu.unhaha.dto;

import commu.unhaha.domain.User;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@Builder
public class WriteArticleForm {

    @NotBlank(message = "게시판을 선택해주세요")
    private String board;

    @NotEmpty(message = "제목을 입력해주세요")
    private String title;

    @NotEmpty(message = "내용을 입력해주세요")
    private String content;

    private User user;

    private Integer viewCount;
}
