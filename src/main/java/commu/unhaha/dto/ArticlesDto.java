package commu.unhaha.dto;

import commu.unhaha.domain.Article;
import commu.unhaha.domain.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArticlesDto {

    private Long id;

    private String board;

    private String title;

    private String content;

    private User user;

    private String thumb;

    private Integer viewCount;

    public ArticlesDto(Article article) {
        this.id = article.getId();
        this.board = article.getBoard();
        this.title = article.getTitle();
        this.content = article.getContent();
        this.user = article.getUser();
        this.viewCount = article.getViewCount();
    }
}
