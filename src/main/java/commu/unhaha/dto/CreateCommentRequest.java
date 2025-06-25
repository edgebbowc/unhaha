package commu.unhaha.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {
    @NotBlank(message = "댓글 내용은 필수입니다.")
    @Size(max = 200, message = "댓글은 200자를 초과할 수 없습니다.")
    private String content;

    private List<String> imageUrl;

    // 부모 댓글
    private Long parentId;
}
