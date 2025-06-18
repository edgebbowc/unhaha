package commu.unhaha.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import commu.unhaha.domain.Article;
import commu.unhaha.domain.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ArticlesDto {

    private Long id;

    private String board;

    private String title;

    private String content;

    private String userNickName;

    private String thumb;

    private Integer viewCount;

    private Integer likeCount;

    private Long commentCount;  // 추가

    private LocalDateTime createdDate;

    private String dateTime;

    // 기본 생성자 (QueryDSL Projections용)
    public ArticlesDto(Long id, String board, String title, String content,
                       String userNickName, Integer viewCount,
                       Integer likeCount, Long commentCount, LocalDateTime createdDate) {
        this.id = id;
        this.board = board;
        this.title = title;
        this.content = content;
        this.userNickName = userNickName;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.createdDate = createdDate;
    }

    public ArticlesDto(Article article) {
        this.id = article.getId();
        this.board = article.getBoard();
        this.title = article.getTitle();
        this.content = article.getContent();
        this.userNickName = article.getUser().getNickname();
        this.viewCount = article.getViewCount();
        this.likeCount = article.getLikeCount();
        this.commentCount = 0L; // 기본값 (별도 조회 필요)
        this.createdDate = article.getCreatedDate();
    }
}
