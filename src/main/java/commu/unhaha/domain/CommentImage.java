package commu.unhaha.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CommentImage extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImageStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    /**
     * 정적 팩토리 메서드: 임시 이미지 생성
     */
    public static CommentImage createTemp(String url) {
        return new CommentImage(null, url, ImageStatus.TEMP, null);
    }

    /**
     * 댓글 등록 시 이미지 상태를 ACTIVE로 변경하고 댓글과 연결
     */
    public void attachToComment(Comment comment) {
        if (this.status != ImageStatus.TEMP) {
            throw new IllegalStateException("TEMP 상태의 이미지가 아닙니다.");
        }
        this.status = ImageStatus.ACTIVE;
        this.comment = comment;
    }

    /**
     * 댓글 수정 시 이미지 삭제 처리
     */
    public void markAsTemp() {
        this.status = ImageStatus.TEMP;
        this.comment = null;
    }
}


