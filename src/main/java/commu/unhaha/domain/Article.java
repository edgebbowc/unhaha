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

    private Integer likeCount;

    public Article(String board, String title, String content, User user, Integer viewCount, Integer likeCount) {
        this.board = board;
        this.title = title;
        this.content = content;
        if (user != null) {
            changeUser(user);
        }
        this.viewCount = viewCount;
        this.likeCount = likeCount;
    }

    private void changeUser(User user) {
        this.user = user;
        user.getArticles().add(this);
    }

    public void changeArticle(String board, String title, String content) {
        this.board = board;
        this.title = title;
        this.content = content;
    }
    public void increaseViewCount() {
        viewCount++;
    }

    public void increaseLikeCount() {
        likeCount++;
    }

    public void decreaseLikeCount() {
        likeCount--;
    }
}
