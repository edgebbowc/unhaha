package commu.unhaha.domain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Article extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    // 좋아요 1개 달성 시점 추가
    @Column(name = "like_achieved_at")
    private LocalDateTime likeAchievedAt;

    @Builder
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

    //연관관계 편의 메소드
    private void changeUser(User user) {
        this.user = user;
        user.getArticles().add(this);
    }

    public void changeArticle(String board, String title, String content) {
        this.board = board;
        this.title = title;
        this.content = content;
    }

    public void increaseLikeCount() {
        likeCount++;

        // 좋아요가 1개가 되는 순간 시점 기록
        if (this.likeCount == 1 && this.likeAchievedAt == null) {
            this.likeAchievedAt = LocalDateTime.now();
        }
    }

    public void decreaseLikeCount() {
        likeCount--;
    }
}
