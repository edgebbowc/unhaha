package commu.unhaha.domain;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ArticleImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImageStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id")
    private Article article;

    /**
     * 정적 팩토리 메서드: 임시 이미지 생성 (글 작성 중 업로드 시 사용)
     */
    public static ArticleImage createTemp(String url) {
        return new ArticleImage(null, url, ImageStatus.TEMP, null);
    }

    /**
     * 글 등록 시 호출하여 이미지 상태를 ACTIVE로 바꾸고 글과 연결
     */
    public void attachToArticle(Article article) {
        if (this.status != ImageStatus.TEMP) {
            throw new IllegalStateException("TEMP 상태의 이미지가 아닙니다.");
        }
        this.status = ImageStatus.ACTIVE;
        this.article = article;
    }

    /**
     * 글 수정 시 원래 있던 이미지 삭제 할 경우 이미지 상태 TEMP로 바꾸고 글과 연결 해제
     */
    public void markAsTemp() {
        this.status = ImageStatus.TEMP;
        this.article = null;
    }
}
