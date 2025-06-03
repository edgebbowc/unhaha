package commu.unhaha.unit.service;

import commu.unhaha.domain.*;
import commu.unhaha.dto.CommentDto;
import commu.unhaha.file.GCSFileStore;
import commu.unhaha.repository.*;
import commu.unhaha.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @Mock
    private CommentImageRepository commentImageRepository;
    @Mock
    private GCSFileStore gcsFileStore;
    @InjectMocks
    private CommentService commentService;

    private User user;
    private Article article;
    private Comment rootComment1;
    private Comment rootComment2;
    private Comment reply1;
    private Comment reply2;
    private Comment nestedReply;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .name("user")
                .nickname("nickname")
                .email("email")
                .role(Role.USER)
                .profileImage(new UploadFile("userImage", "userImage"))
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        article = Article.builder()
                .board("board")
                .title("title")
                .content("content")
                .user(user)
                .viewCount(0)
                .likeCount(0)
                .build();

        // 루트 댓글들
        rootComment1 = Comment.builder()
                .content("루트 댓글 1")
                .user(user)
                .article(article)
                .parent(null)
                .build();
        ReflectionTestUtils.setField(rootComment1, "id", 1L);
        ReflectionTestUtils.setField(rootComment1, "createdDate", LocalDateTime.now().minusHours(3));

        rootComment2 = Comment.builder()
                .content("루트 댓글 2")
                .user(user)
                .article(article)
                .parent(null)
                .build();
        ReflectionTestUtils.setField(rootComment2, "id", 2L);
        ReflectionTestUtils.setField(rootComment2, "createdDate", LocalDateTime.now().minusHours(2));

        // 대댓글들
        reply1 = Comment.builder()
                .content("대댓글 1")
                .user(user)
                .article(article)
                .parent(rootComment1)
                .build();
        ReflectionTestUtils.setField(reply1, "id", 3L);
        ReflectionTestUtils.setField(reply1, "createdDate", LocalDateTime.now().minusHours(1));

        reply2 = Comment.builder()
                .content("대댓글 2")
                .user(user)
                .article(article)
                .parent(rootComment2)
                .build();
        ReflectionTestUtils.setField(reply2, "id", 4L);
        ReflectionTestUtils.setField(reply2, "createdDate", LocalDateTime.now().minusMinutes(30));

        // 중첩 대댓글 (대댓글의 대댓글)
        nestedReply = Comment.builder()
                .content("중첩 대댓글")
                .user(user)
                .article(article)
                .parent(reply1)
                .build();
        ReflectionTestUtils.setField(nestedReply, "id", 5L);
        ReflectionTestUtils.setField(nestedReply, "createdDate", LocalDateTime.now());
    }

    @Test
    @DisplayName("댓글 작성")
    void createComment() {
        //when
        when(articleRepository.findById(10L)).thenReturn(Optional.of(article));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Comment comment = commentService.createComment(user, 10L, "댓글 내용", null, null);

        assertThat(comment.getContent()).isEqualTo("댓글 내용");
        assertThat(comment.getUser()).isEqualTo(user);
        assertThat(comment.getArticle()).isEqualTo(article);
        assertThat(comment.getParent()).isNull();
    }

    @Test
    @DisplayName("이미지 포함 댓글 작성")
    void createCommentWithImages() {
        // given
        List<String> imageUrls = List.of("image1.jpg", "image2.jpg");
        when(articleRepository.findById(10L)).thenReturn(Optional.of(article));
        when(commentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(commentImageRepository.findByUrlInAndStatus(any(), eq(ImageStatus.TEMP)))
                .thenReturn(List.of(
                        CommentImage.createTemp("image1.jpg"),
                        CommentImage.createTemp("image2.jpg")
                ));

        // when
        Comment result = commentService.createComment(user, 10L, "내용", imageUrls, null);
        commentRepository.flush();

        // then
        assertThat(result.getContent()).contains("![이미지](image1.jpg)");
        verify(commentImageRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("대댓글 작성")
    void createChildComment() {
        when(articleRepository.findById(10L)).thenReturn(Optional.of(article));
        when(commentRepository.findParentWithRelations(1L)).thenReturn(java.util.Optional.of(rootComment1));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Comment child = commentService.createComment(user, 10L, "대댓글",null, 1L);

        assertThat(child.getContent()).isEqualTo("대댓글");
        assertThat(child.getParent()).isEqualTo(rootComment1);
        verify(commentRepository).findParentWithRelations(1L); // Fetch Join 사용 검증
    }

    @Test
    @DisplayName("댓글 수정")
    void updateComment() {
        // given
        when(commentRepository.findById(1L)).thenReturn(Optional.of(rootComment1));

        // when
        commentService.updateComment(1L, "수정된 내용", null);

        // then
        assertThat(rootComment1.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("댓글 수정 - 이미지 추가/삭제")
    void updateCommentWithImages() {
        // given
        List<String> newImageUrls = List.of("new-image.jpg");

        when(commentRepository.findById(1L)).thenReturn(Optional.of(rootComment1));
        when(commentImageRepository.findByCommentId(1L))
                .thenReturn(List.of(
                        CommentImage.builder().url("old-image.jpg").status(ImageStatus.ACTIVE).build()
                ));
        when(commentImageRepository.findByUrlAndStatus("new-image.jpg", ImageStatus.TEMP))
                .thenReturn(Optional.of(CommentImage.createTemp("new-image.jpg")));

        // when
        commentService.updateComment(1L, "새 내용", newImageUrls);

        // then
        assertThat(rootComment1.getContent()).contains("새 내용");
        assertThat(rootComment1.getContent()).contains("![이미지](new-image.jpg)");
        ArgumentCaptor<CommentImage> saveCaptor = ArgumentCaptor.forClass(CommentImage.class);
        verify(commentImageRepository, atLeastOnce()).save(saveCaptor.capture());

        List<CommentImage> savedImages = saveCaptor.getAllValues();
        assertThat(savedImages).hasSize(1);
        assertThat(savedImages.get(0).getUrl()).isEqualTo("new-image.jpg");
        assertThat(savedImages.get(0).getStatus()).isEqualTo(ImageStatus.ACTIVE);
    }


    @Test
    @DisplayName("댓글 삭제")
    void deleteComment() {
        // given
        when(commentRepository.findById(1L)).thenReturn(Optional.of(rootComment1));

        // when
        commentService.deleteComment(1L);

        // then
        verify(commentRepository).delete(rootComment1);
    }

    @Test
    @DisplayName("댓글 삭제 - 이미지 포함")
    void deleteCommentWithImages() {
        // given
        when(commentRepository.findById(1L)).thenReturn(Optional.of(rootComment1));
        when(commentImageRepository.findUrlsByCommentIdIn(anySet()))
                .thenReturn(List.of("image1.jpg", "image2.jpg"));

        // when
        commentService.deleteComment(1L);

        // then
        verify(gcsFileStore, times(2)).deleteFile(anyString());
        verify(commentRepository).delete(rootComment1);
    }

    @Test
    @DisplayName("좋아요 추가 테스트")
    void saveLikeTest() {
        // given
        when(commentRepository.findById(1L)).thenReturn(Optional.of(rootComment1));
        when(userLikeCommentRepository.existsByCommentIdAndUserId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        commentService.saveLike(1L, 1L);

        // then
        verify(userLikeCommentRepository).save(any());
        assertThat(rootComment1.getLikeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("좋아요 취소 테스트")
    void removeLikeTest() {
        // given
        rootComment1.increaseLikeCount();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(rootComment1));
        when(userLikeCommentRepository.existsByCommentIdAndUserId(1L, 1L)).thenReturn(true);

        // when
        commentService.saveLike(1L, 1L);

        // then
        assertThat(rootComment1.getLikeCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("존재하지 않는 부모 댓글 예외 테스트")
    void createCommentWithInvalidParent() {
        // given
        when(articleRepository.findById(10L)).thenReturn(Optional.of(article));
        when(commentRepository.findParentWithRelations(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.createComment(user, 10L, "내용", null, 999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("부모 댓글");
    }

    @Test
    @DisplayName("페이지 계산 로직 테스트")
    void calculatePageIndexTest() {
        // given
        when(commentRepository.countByArticleIdAndParentIsNull(10L))
                .thenReturn(15L); // 15개 댓글, 페이지당 2개, 총 페이지수 8개

        // when
        int pageIndex = commentService.calculatePageIndex(10L, 3); // 1-based 3페이지 요청
        int lastPageIndex = commentService.calculatePageIndex(10L, 10); // 1-based 존재하지 않는 페이지 요청
        int nullPage = commentService.calculatePageIndex(10L, null);

        // then (페이지 인덱스는 0-based)
        assertThat(pageIndex).isEqualTo(2); // 3페이지 → 인덱스 2
        assertThat(lastPageIndex).isEqualTo(7); // 마지막 페이지 반환
        assertThat(nullPage).isEqualTo(7); // page가 null -> 마지막 페이지 반환
    }

    @Test
    @DisplayName("루트 댓글이 없는 경우 빈 페이지 반환")
    void commentPageList_EmptyRootComments_ReturnsEmptyPage() {
        // given
        Long articleId = 100L;
        int page = 0;
        Pageable pageable = PageRequest.of(page, 2, Sort.by("createdDate").ascending());
        Page<Comment> emptyPage = Page.empty(pageable);

        when(commentRepository.findByArticleIdAndParentIsNullOrderByCreatedDateAsc(articleId, pageable))
                .thenReturn(emptyPage);

        // when
        Page<CommentDto> result = commentService.commentPageList(articleId, page);

        // then
        assertThat(result).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(commentRepository).findByArticleIdAndParentIsNullOrderByCreatedDateAsc(articleId, pageable);
        verify(commentRepository, never()).findRepliesWithUserByArticleId(any());
    }

    @Test
    @DisplayName("루트 댓글만 있는 경우 (대댓글 없음)")
    void commentPageList_OnlyRootComments_ReturnsRootCommentsOnly() {
        // given
        Long articleId = 100L;
        int page = 0;
        Pageable pageable = PageRequest.of(page, 2, Sort.by("createdDate").ascending());
        Page<Comment> rootPage = new PageImpl<>(List.of(rootComment1, rootComment2), pageable, 2);

        when(commentRepository.findByArticleIdAndParentIsNullOrderByCreatedDateAsc(articleId, pageable))
                .thenReturn(rootPage);
        when(commentRepository.findRepliesWithUserByArticleId(articleId))
                .thenReturn(List.of()); // 대댓글 없음

        // when
        Page<CommentDto> result = commentService.commentPageList(articleId, page);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("루트 댓글 1");
        assertThat(result.getContent().get(1).getContent()).isEqualTo("루트 댓글 2");
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(commentRepository).findByArticleIdAndParentIsNullOrderByCreatedDateAsc(articleId, pageable);
        verify(commentRepository).findRepliesWithUserByArticleId(articleId);
    }

    @Test
    @DisplayName("루트 댓글과 대댓글이 모두 있는 경우")
    void commentPageList_WithRootAndReplies_ReturnsFlattedList() {
        // given
        Long articleId = 100L;
        int page = 0;
        Pageable pageable = PageRequest.of(page, 2, Sort.by("createdDate").ascending());
        Page<Comment> rootPage = new PageImpl<>(List.of(rootComment1, rootComment2), pageable, 2);

        when(commentRepository.findByArticleIdAndParentIsNullOrderByCreatedDateAsc(articleId, pageable))
                .thenReturn(rootPage);
        when(commentRepository.findRepliesWithUserByArticleId(articleId))
                .thenReturn(List.of(reply1, reply2));

        // when
        Page<CommentDto> result = commentService.commentPageList(articleId, page);

        // then
        assertThat(result.getContent()).hasSize(4); // 루트 2개 + 대댓글 2개

        // 평면화된 순서 확인: 루트1 -> 대댓글1 -> 루트2 -> 대댓글2
        assertThat(result.getContent().get(0).getContent()).isEqualTo("루트 댓글 1");
        assertThat(result.getContent().get(0).isReply()).isFalse();

        assertThat(result.getContent().get(1).getContent()).isEqualTo("대댓글 1");
        assertThat(result.getContent().get(1).isReply()).isTrue();
        assertThat(result.getContent().get(1).getParentId()).isEqualTo(1L);

        assertThat(result.getContent().get(2).getContent()).isEqualTo("루트 댓글 2");
        assertThat(result.getContent().get(2).isReply()).isFalse();

        assertThat(result.getContent().get(3).getContent()).isEqualTo("대댓글 2");
        assertThat(result.getContent().get(3).isReply()).isTrue();
        assertThat(result.getContent().get(3).getParentId()).isEqualTo(2L);

        assertThat(result.getTotalElements()).isEqualTo(2); // 루트 댓글 기준 총 개수
    }

    @Test
    @DisplayName("중첩된 대댓글(대댓글의 대댓글)이 있는 경우")
    void commentPageList_WithNestedReplies_ReturnsCorrectOrder() {
        // given
        Long articleId = 100L;
        int page = 0;
        Pageable pageable = PageRequest.of(page, 2, Sort.by("createdDate").ascending());
        Page<Comment> rootPage = new PageImpl<>(List.of(rootComment1), pageable, 1);

        when(commentRepository.findByArticleIdAndParentIsNullOrderByCreatedDateAsc(articleId, pageable))
                .thenReturn(rootPage);
        when(commentRepository.findRepliesWithUserByArticleId(articleId))
                .thenReturn(List.of(reply1, nestedReply));

        // when
        Page<CommentDto> result = commentService.commentPageList(articleId, page);

        // then
        assertThat(result.getContent()).hasSize(3); // 루트1 + 대댓글1 + 중첩대댓글

        assertThat(result.getContent().get(0).getContent()).isEqualTo("루트 댓글 1");
        assertThat(result.getContent().get(0).isReply()).isFalse();

        assertThat(result.getContent().get(1).getContent()).isEqualTo("대댓글 1");
        assertThat(result.getContent().get(1).isReply()).isTrue();
        assertThat(result.getContent().get(1).getParentId()).isEqualTo(1L);

        assertThat(result.getContent().get(2).getContent()).isEqualTo("중첩 대댓글");
        assertThat(result.getContent().get(2).isReply()).isTrue();
        assertThat(result.getContent().get(2).getParentId()).isEqualTo(3L);
        assertThat(result.getContent().get(2).isNestedReply()).isTrue();
    }

    @Test
    @DisplayName("페이지 크기와 정렬 순서 확인")
    void commentPageList_PageSizeAndSorting_ReturnsCorrectPageable() {
        // given
        Long articleId = 100L;
        int page = 1; // 두 번째 페이지
        Pageable expectedPageable = PageRequest.of(page, 2, Sort.by("createdDate").ascending());
        Page<Comment> rootPage = new PageImpl<>(List.of(rootComment2), expectedPageable, 3);

        when(commentRepository.findByArticleIdAndParentIsNullOrderByCreatedDateAsc(articleId, expectedPageable))
                .thenReturn(rootPage);
        when(commentRepository.findRepliesWithUserByArticleId(articleId))
                .thenReturn(List.of());

        // when
        Page<CommentDto> result = commentService.commentPageList(articleId, page);

        // then
        assertThat(result.getPageable().getPageNumber()).isEqualTo(1);
        assertThat(result.getPageable().getPageSize()).isEqualTo(2);
        assertThat(result.getPageable().getSort()).isEqualTo(Sort.by("createdDate").ascending());

        verify(commentRepository).findByArticleIdAndParentIsNullOrderByCreatedDateAsc(articleId, expectedPageable);
    }
}

