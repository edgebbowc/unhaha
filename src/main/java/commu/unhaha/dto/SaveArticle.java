package commu.unhaha.dto;

import commu.unhaha.domain.User;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class SaveArticle {

    private String board;

    private String title;

    private String content;

    private User user;
}
