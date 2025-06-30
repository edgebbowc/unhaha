package commu.unhaha.dto;

import commu.unhaha.domain.Article;
import commu.unhaha.domain.BoardType;
import commu.unhaha.domain.User;
import lombok.Data;

import java.time.LocalDateTime;

import static commu.unhaha.domain.BoardType.*;
@Data
public class ArticleDto {

    private Long id;

    private String board;

    private String boardPath;

    private String title;

    private String content;

    private Long userId;

    private String userNickname;

    private String userEmail;

    private Integer viewCount;

    private Integer likeCount;

    private LocalDateTime createdDate;

    private LocalDateTime likeAchievedAt;

    private String dateTime;

    public ArticleDto(Article article) {
        this.id = article.getId();
        this.board = article.getBoard();
        this.boardPath = titleToPath(article.getBoard());
        this.title = article.getTitle();
        this.content = article.getContent();
        this.userId = article.getUser().getId();
        this.userNickname = article.getUser().getNickname();
        this.userEmail = article.getUser().getEmail();
        this.viewCount = article.getViewCount();
        this.likeCount = article.getLikeCount();
        this.createdDate = article.getCreatedDate();
        this.likeAchievedAt = article.getLikeAchievedAt();
    }

}
