package commu.unhaha.domain;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Article extends BaseTimeEntity{

    @Id @GeneratedValue
    @Column(name = "article_id")
    private Long id;

    private String board;

    private String title;

    @Column(length = 50000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Integer viewCount;

    public Article(String board, String title, String content, User user, Integer viewCount) {
        this.board = board;
        this.title = title;
        this.content = content;
        this.user = user;
        this.viewCount = viewCount;
    }

    public void changeArticle(String board, String title, String content) {
        this.board = board;
        this.title = title;
        this.content = content;
    }
    public void increaseViewCount() {
        viewCount++;
    }
}
