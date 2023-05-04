package commu.unhaha.dto;

import commu.unhaha.domain.Article;
import commu.unhaha.domain.User;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Getter
@Setter
public class ArticleDto {

    private Long id;

    private String board;

    private String title;

    private String content;

    private User user;

    private String thumb;

    public ArticleDto(Article article) {
        this.id = article.getId();
        this.board = article.getBoard();
        this.title = article.getTitle();
        this.content = article.getContent();
        this.user = article.getUser();
    }
}
