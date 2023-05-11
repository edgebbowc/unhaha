package commu.unhaha.dto;

import commu.unhaha.domain.Article;
import commu.unhaha.domain.User;
import lombok.Data;

@Data
public class ArticleDto {

    private Long id;

    private String board;

    private String title;

    private String content;

    private User user;

    private Integer viewCount;

    public ArticleDto(Article article) {
        this.id = article.getId();
        this.board = article.getBoard();
        this.title = article.getTitle();
        this.content = article.getContent();
        this.user = article.getUser();
        this.viewCount = article.getViewCount();
    }
}
