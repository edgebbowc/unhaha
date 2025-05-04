package commu.unhaha.service;

import commu.unhaha.domain.*;
import commu.unhaha.dto.ArticlesDto;
import commu.unhaha.dto.CommentDto;
import commu.unhaha.repository.ArticleRepository;
import commu.unhaha.repository.CommentRepository;
import commu.unhaha.repository.UserLikeCommentRepository;
import commu.unhaha.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final UserLikeCommentRepository userLikeCommentRepository;

    public Comment createComment(User user, Long articleId, String content, Long parentId) {
        Article article = articleRepository.findById(articleId).orElseThrow();
        Comment parent = parentId == null ? null : commentRepository.findById(parentId).orElseThrow();
        Comment comment = Comment.builder()
                .content(content)
                .user(user)
                .article(article)
                .parent(parent)
                .build();
        return commentRepository.save(comment);
    }

    public void updateComment(Long commentId, String content) {
        Comment comment = commentRepository.findById(commentId).orElseThrow();
        comment.changeContent(content);
    }

    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow();
        commentRepository.delete(comment);
    }

    // 댓글 페이징
    public Page<CommentDto> commentPageList(Long articleId, int page) {
        Pageable pageable = PageRequest.of(page, 30, Sort.by("createdDate").ascending());
        // 루트 댓글(부모 없는 댓글) 페이징 조회
        Page<Comment> rootComments  = commentRepository.findByArticleIdAndParentIsNullOrderByCreatedDateAsc(articleId, pageable);

        // 대댓글 일괄 조회
        List<Comment> replies = commentRepository.findByParentInOrderByCreatedDateAsc(rootComments.getContent());

        // 대댓글을 Map<부모ID, List<대댓글>> 구조로 정리
        Map<Long, List<Comment>> replyMap = replies.stream()
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        // 루트 + 자식 트리 구조로 DTO 변환
        List<CommentDto> commentTrees = rootComments.getContent().stream()
                .map(parent -> toTreeDto(parent, replyMap.get(parent.getId())))
                .collect(Collectors.toList());

        return new PageImpl<>(commentTrees, pageable, rootComments.getTotalElements());
    }

    private CommentDto toTreeDto(Comment parent, List<Comment> children) {
        CommentDto dto = new CommentDto(parent);
        if (children != null) {
            List<CommentDto> childDtos = children.stream()
                    .map(CommentDto::new)
                    .collect(Collectors.toList());
            dto.setChildren(childDtos); // children 필드 추가 필요
        }
        return dto;
    }

    // 댓글 좋아요 확인
    public boolean findLike(Long commentId, Long userId) {
        return userLikeCommentRepository.existsByCommentIdAndUserId(commentId, userId);
    }

    public boolean saveLike(Long commentId, Long userId) {

        /** 로그인한 유저가 해당 게시물을 좋아요 했는지 안 했는지 확인 **/
        if(!findLike(commentId, userId)){
            /* 좋아요 하지 않은 댓글이면 좋아요 추가, true 반환 */
            User user = userRepository.findById(userId).orElse(null);
            Comment comment = validateAndGetComment(commentId);

            /* UserLikeComment 엔티티 생성 */
            UserLikeComment userLikeComment = new UserLikeComment(user, comment);
            userLikeCommentRepository.save(userLikeComment);
            comment.increaseLikeCount();

            return true;
        } else {
            /* 좋아요 한 댓글이면 좋아요 삭제 */
            Comment comment = validateAndGetComment(commentId);
            userLikeCommentRepository.deleteByCommentIdAndUserId(commentId, userId);
            comment.decreaseLikeCount();

            return false;
        }
    }

    public Comment validateAndGetComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 댓글이 존재하지 않습니다."));
    }
}
