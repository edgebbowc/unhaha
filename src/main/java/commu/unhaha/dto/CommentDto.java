package commu.unhaha.dto;


import commu.unhaha.domain.Comment;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CommentDto {
    private Long id;

    private String content;

    private String nickname;

    private String storeFileUrl;

    private Integer likeCount;

    private LocalDateTime createdDate;

    private String dateTime;

    private List<CommentDto> children = new ArrayList<>();

    private boolean isReply;

    public CommentDto(Comment comment) {
        this.id = comment.getId();
        this.content = comment.getContent();
        this.nickname = comment.getUser().getNickname();
        this.storeFileUrl = comment.getUser().getProfileImage().getStoreFileUrl();
        this.likeCount = comment.getLikeCount();
        this.createdDate = comment.getCreatedDate();
        this.isReply = comment.getParent() != null;
    }
}
