package commu.unhaha.unit.service;

import commu.unhaha.domain.*;
import commu.unhaha.dto.CommentDto;
import commu.unhaha.repository.ArticleRepository;
import commu.unhaha.repository.CommentRepository;
import commu.unhaha.repository.UserLikeCommentRepository;
import commu.unhaha.repository.UserRepository;
import commu.unhaha.service.CommentService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ArticleRepository articleRepository;
    @Mock
    private UserLikeCommentRepository userLikeCommentRepository;

    @InjectMocks
    private CommentService commentService;

    private User user;
    private Article article;
    private Comment parentComment;
    private Comment parentComment2;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .name("user")
                .nickname("nickname")
                .email("email")
                .role(Role.USER)
                .profileImage(new UploadFile("userImage", "userImage"))
                .build();
        article = Article.builder()
                .board("board")
                .title("title")
                .content("content")
                .user(user)
                .viewCount(0)
                .likeCount(0)
                .build();
        parentComment = Comment.builder()
                .content("parent")
                .user(user)
                .article(article)
                .parent(null)
                .build();
        parentComment2 = Comment.builder()
                .content("parent2")
                .user(user)
                .article(article)
                .parent(null)
                .build();
    }

    @Test
    @DisplayName("댓글 작성")
    void createComment() {
        //when
        when(articleRepository.findById(10L)).thenReturn(Optional.of(article));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Comment comment = commentService.createComment(user, 10L, "댓글 내용", null);

        assertThat(comment.getContent()).isEqualTo("댓글 내용");
        assertThat(comment.getUser()).isEqualTo(user);
        assertThat(comment.getArticle()).isEqualTo(article);
        assertThat(comment.getParent()).isNull();
    }

    @Test
    @DisplayName("대댓글 작성")
    void createChildComment() {
        when(articleRepository.findById(10L)).thenReturn(java.util.Optional.of(article));
        when(commentRepository.findById(100L)).thenReturn(java.util.Optional.of(parentComment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Comment child = commentService.createComment(user, 10L, "대댓글", 100L);

        assertThat(child.getContent()).isEqualTo("대댓글");
        assertThat(child.getParent()).isEqualTo(parentComment);
    }

    @Test
    @DisplayName("댓글 수정")
    void updateComment() {
        Comment comment = Comment.builder()
                .content("원본")
                .user(user)
                .article(article)
                .build();

        when(commentRepository.findById(200L)).thenReturn(java.util.Optional.of(comment));

        commentService.updateComment(200L, "수정된 내용");

        assertThat(comment.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("댓글 삭제")
    void deleteComment() {
        Comment comment = Comment.builder()
                .content("댓글")
                .user(user)
                .article(article)
                .build();

        when(commentRepository.findById(300L)).thenReturn(java.util.Optional.of(comment));

        commentService.deleteComment(300L);

        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("댓글 페이징 목록 조회")
    void commentPageListTest() {
        // given
        Long articleId = 10L;
        List<Comment> comments = List.of(parentComment, parentComment2);
        Page<Comment> commentPage = new PageImpl<>(comments);

        when(commentRepository.findByArticleIdAndParentIsNullOrderByCreatedDateAsc(articleId, PageRequest.of(0, 30, Sort.by("createdDate").ascending())))
                .thenReturn(commentPage);

        // when
        Page<CommentDto> result = commentService.commentPageList(articleId, 0);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("parent");
        assertThat(result.getContent().get(0).getNickname()).isEqualTo("nickname");
        assertThat(result.getContent().get(0).getStoreFileUrl()).isEqualTo("userImage");
        assertThat(result.getContent().get(1).getContent()).isEqualTo("parent2");
    }
}
