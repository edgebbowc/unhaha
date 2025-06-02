package commu.unhaha.dto;


import commu.unhaha.domain.Comment;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CommentDto {
    private Long id;

    private Long parentId;

    private Long articleId;

    private Long userId;

    private String content;

    private List<String> imageUrls; // 이미지 URL 목록

    private String nickname;

    private String storeFileUrl;

    private Integer likeCount;

    private LocalDateTime createdDate;

    private String dateTime;

    private List<CommentDto> children = new ArrayList<>();

    private boolean isReply;

    private boolean isNestedReply;      // 대댓글의 대댓글 여부
    private String parentNickname;      // 부모 댓글 작성자 닉네임
    private boolean isParentAuthor;     // 부모 댓글 작성자가 게시글 작성자인지 여부

    public CommentDto(Comment comment) {
        this.id = comment.getId();
        this.articleId = comment.getArticle().getId();
        this.userId = comment.getUser().getId();
        this.content = comment.getContent();
        this.nickname = comment.getUser().getNickname();
        this.storeFileUrl = comment.getUser().getProfileImage().getStoreFileUrl();
        this.likeCount = comment.getLikeCount();
        this.createdDate = comment.getCreatedDate();
        if (comment.getParent() != null) {
            this.parentId = comment.getParent().getId();
            this.isReply = true;
            // 대댓글의 대댓글 여부 및 부모 닉네임 설정
            Comment parent = comment.getParent();
            if (parent.getParent() != null) {
                // 대댓글의 대댓글인 경우
                this.isNestedReply = true;
                this.parentNickname = parent.getUser().getNickname();

                // 부모 댓글 작성자가 댓글 작성자인지 확인
                this.isParentAuthor = parent.getUser().getId()
                        .equals(comment.getUser().getId());
            } else {
                // 루트 댓글의 대댓글인 경우
                this.isNestedReply = false;
                this.parentNickname = null;
                this.isParentAuthor = false;
            }
        } else {
            this.parentId = null;
            this.isReply = false;
            this.isNestedReply = false;
            this.parentNickname = null;
            this.isParentAuthor = false;
        }
    }
}
