package commu.unhaha.dto;

import commu.unhaha.domain.Article;
import commu.unhaha.domain.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleDto {

    private Long id;

    private String board;

    private String title;

    private String content;

    private User user;

    private Integer viewCount;

    private Integer likeCount;

    private LocalDateTime createdDate;

    private LocalDateTime likeAchievedAt;

    private String dateTime;

    public ArticleDto(Article article) {
        this.id = article.getId();
        this.board = article.getBoard();
        this.title = article.getTitle();
        this.content = article.getContent();
        this.user = article.getUser();
        this.viewCount = article.getViewCount();
        this.likeCount = article.getLikeCount();
        this.createdDate = article.getCreatedDate();
        this.likeAchievedAt = article.getLikeAchievedAt();
    }
}
