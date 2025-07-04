package commu.unhaha.unit.service;

import commu.unhaha.domain.*;
import commu.unhaha.dto.ArticlesDto;
import commu.unhaha.dto.WriteArticleForm;
import commu.unhaha.file.GCSFileStore;
import commu.unhaha.repository.ArticleImageRepository;
import commu.unhaha.repository.ArticleRepository;
import commu.unhaha.repository.UserLikeArticleRepository;
import commu.unhaha.repository.UserRepository;
import commu.unhaha.service.ArticleService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ArticleImageRepository articleImageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserLikeArticleRepository userLikeArticleRepository;

    @Mock
    private GCSFileStore gcsFileStore;

    @InjectMocks
    private ArticleService articleService;

    private User user;
    private Article article;
    private User otherUser;

    @BeforeEach
    void setUp() {

        user = User.builder()
                .nickname("nickname")
                .email("email")
                .role(Role.USER)
                .profileImage(new UploadFile("userImage", "userImage"))
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        otherUser = User.builder()
                .nickname("otherUser")
                .email("other@example.com")
                .role(Role.USER)
                .profileImage(new UploadFile("userImage", "userImage"))
                .build();
        ReflectionTestUtils.setField(otherUser, "id", 2L);

        article = Article.builder()
                .board("board")
                .title("title")
                .content("content")
                .user(user)
                .viewCount(0)
                .likeCount(0)
                .build();
        ReflectionTestUtils.setField(article, "id", 1L);
    }
    private ArticleImage createMockArticleImage(String url) {
        ArticleImage image = mock(ArticleImage.class);
        // attachToArticle stub 설정
        doNothing().when(image).attachToArticle(any(Article.class));
        return image;
    }

    @Test
    @DisplayName("게시글 생성 - 이미지 포함")
    void createArticle_shouldSaveArticleAndAttachImages() {
        // given
        WriteArticleForm form = WriteArticleForm.builder()
                .board("보디빌딩")
                .title("테스트 제목")
                .content("테스트 내용 <img src=\"url1\"> <img src=\"url2\">")
                .build();

        Set<String> imageUrls = Set.of("url1", "url2");
        List<ArticleImage> images = Arrays.asList(
                createMockArticleImage("url1"),
                createMockArticleImage("url2")
        );

        when(articleRepository.save(any(Article.class))).thenReturn(article);
        when(articleImageRepository.findByUrlIn(imageUrls)).thenReturn(images);

        // when
        Long articleId = articleService.createArticle(form, user);

        // then
        assertThat(articleId).isEqualTo(1L);

        // Article 저장 검증
        ArgumentCaptor<Article> articleCaptor = ArgumentCaptor.forClass(Article.class);
        verify(articleRepository).save(articleCaptor.capture());

        Article capturedArticle = articleCaptor.getValue();
        assertThat(capturedArticle.getBoard()).isEqualTo("보디빌딩");
        assertThat(capturedArticle.getTitle()).isEqualTo("테스트 제목");
        assertThat(capturedArticle.getContent()).isEqualTo("테스트 내용 <img src=\"url1\"> <img src=\"url2\">");
        assertThat(capturedArticle.getUser()).isEqualTo(user);
        assertThat(capturedArticle.getViewCount()).isEqualTo(0);
        assertThat(capturedArticle.getLikeCount()).isEqualTo(0);

        // 이미지 처리 검증
        verify(articleImageRepository).findByUrlIn(imageUrls);
        for (ArticleImage image : images) {
            verify(image).attachToArticle(article);
        }
        verify(articleImageRepository).saveAll(images);
    }

    @DisplayName("게시판 수정")
    @Test
    public void editArticle() throws Exception {
        //given
        User user = new User( "nickname", "email", Role.USER, new UploadFile("userimage", "userimage"));
        Article article = new Article("board", "title", "content", user, 0, 0);
        given(articleRepository.findById(1L)).willReturn(Optional.of(article));

        //when
        Article findArticle = articleRepository.findById(1L).orElseThrow();
        findArticle.changeArticle("change", "change", "change");

        //then
        assertThat(findArticle.getBoard()).isEqualTo("change");
        assertThat(findArticle.getTitle()).isEqualTo("change");
        assertThat(findArticle.getContent()).isEqualTo("change");
    }

    @Test
    @DisplayName("게시판 수정 이미지 포함")
    void editArticle_WithNewImages_ShouldActivateNewImages() {
        // given
        Long articleId = 1L;
        String userEmail = "email";
        WriteArticleForm form = WriteArticleForm.builder()
                .board("보디빌딩")
                .title("제목")
                .content("기존 내용 <img style=\"aspect-ratio:150/150;\" src=\"new-image.jpg\" width=\"150\" height=\"150\">")
                .build();

        // TEMP 상태인 새 이미지 Mock 설정
        ArticleImage tempImage = ArticleImage.createTemp("new-image.jpg");

        when(articleRepository.findById(1L)).thenReturn(Optional.ofNullable(article));
        when(articleImageRepository.findByArticleId(1L)).thenReturn(List.of());
        when(articleImageRepository.findByUrlInAndStatus(anyList(), eq(ImageStatus.TEMP)))
                .thenReturn(List.of(tempImage));

        // when
        articleService.editArticle(articleId, form, userEmail);

        // then
        assertThat(tempImage.getStatus()).isEqualTo(ImageStatus.ACTIVE);
    }

    @DisplayName("게시판 삭제")
    @Test
    public void deleteArticle() throws Exception {
        // given
        Long articleId = 1L;
        String userEmail = "email";

        List<ArticleImage> mockImages = List.of(
                ArticleImage.createTemp("url1"),
                ArticleImage.createTemp("url2")
        );

        // validateArticle() 정상 통과하게 모킹
        when(articleRepository.findById(articleId)).thenReturn(Optional.ofNullable(article));

        // findByArticleId() 모킹
        when(articleImageRepository.findByArticleId(articleId)).thenReturn(mockImages);

        // gcsFileStore.deleteFile() 모킹
        doNothing().when(gcsFileStore).deleteFile(anyString());

        // when
        articleService.deleteArticle(articleId, userEmail);

        // then
        // gcsFileStore.deleteFile() 호출된 URL들 캡쳐
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(gcsFileStore, times(2)).deleteFile(captor.capture());
        List<String> deletedUrls = captor.getAllValues();

        assertThat(deletedUrls).containsExactlyInAnyOrder("url1", "url2");

        // articleImageRepository.deleteAll() 제대로 호출됐는지 검증
        ArgumentCaptor<List<ArticleImage>> deleteAllCaptor = ArgumentCaptor.forClass(List.class);
        verify(articleImageRepository).deleteAll(deleteAllCaptor.capture());
        List<ArticleImage> deletedImages = deleteAllCaptor.getValue();

        assertThat(deletedImages).hasSize(2);
        assertThat(deletedImages).extracting("url")
                .containsExactlyInAnyOrder("url1", "url2");

        // articleRepository.deleteById() 호출 확인
        ArgumentCaptor<Long> deleteByIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(articleRepository).deleteById(deleteByIdCaptor.capture());
        Long deletedId = deleteByIdCaptor.getValue();

        assertThat(deletedId).isEqualTo(articleId);
    }

    @Test
    @DisplayName("게시글 좋아요 추가 - 성공")
    void saveLike_addLike_success() {
        // given
        Long articleId = 1L;
        Long userId = 2L; // 다른 사용자
        assertThat(article.getLikeCount()).isEqualTo(0);
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(userLikeArticleRepository.existsByArticleIdAndUserId(articleId, userId)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(otherUser));

        // when
        boolean result = articleService.saveLike(articleId, userId);

        // then
        assertThat(result).isTrue();
        assertThat(article.getLikeCount()).isEqualTo(1);
        verify(userLikeArticleRepository).save(any(UserLikeArticle.class));
    }

    @Test
    @DisplayName("게시글 좋아요 취소 - 성공")
    void saveLike_removeLike_success() {
        // given
        Long articleId = 1L;
        Long userId = 2L;

        // 초기 likeCount를 1로 설정 (이미 좋아요한 상태)
        ReflectionTestUtils.setField(article, "likeCount", 1);
        assertThat(article.getLikeCount()).isEqualTo(1);

        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(userLikeArticleRepository.existsByArticleIdAndUserId(articleId, userId)).thenReturn(true);

        // when
        boolean result = articleService.saveLike(articleId, userId);

        // then
        assertThat(result).isFalse();

        // likeCount가 1 감소했는지 확인
        assertThat(article.getLikeCount()).isEqualTo(0);

        verify(userLikeArticleRepository).deleteByArticleIdAndUserId(articleId, userId);
    }

    @DisplayName("검색조건X 페이징")
    @Test
    public void findAllArticlesNotSearch() throws Exception {
        //given
        Article article1 = new Article("board", "title", "content", user, 0, 0);
        Article article2 = new Article("board", "title", "content", user, 0, 0);
        ReflectionTestUtils.setField(article1, "id", 1L);
        ReflectionTestUtils.setField(article2, "id", 2L);

        int page = 0;
        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "id"));
        List<ArticlesDto> articlesDtoList = Arrays.asList(
                new ArticlesDto(1L, "board", "title", "content", "nickname", 0, 0, 0L, LocalDateTime.now()),
                new ArticlesDto(2L, "board", "title", "content", "nickname", 0, 0, 0L, LocalDateTime.now())
        );
        PageImpl<ArticlesDto> articlesDtoPageImpl = new PageImpl<>(articlesDtoList, pageable, articlesDtoList.size());

        given(articleRepository.findAllArticles(null, null, pageable))
                .willReturn(articlesDtoPageImpl);

        //when
        Page<ArticlesDto> articlesDtos = articleService.findAllArticles(page, null, null);

        //then
        assertThat(articlesDtos.getTotalElements()).isEqualTo(2);
        assertThat(articlesDtos.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "id"));
    }

    @DisplayName("검색조건O 페이징")
    @Test
    public void findAllArticlesSearch() throws Exception {
        //given
        int page = 0;
        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "id"));

        // 제목 검색 결과
        List<ArticlesDto> titleSearchResult = Arrays.asList(
                new ArticlesDto(1L, "board", "title", "content1", "nickname", 0, 0, 0L, LocalDateTime.now()),
                new ArticlesDto(2L, "board", "title", "content2", "nickname", 0, 0, 0L, LocalDateTime.now())
        );
        PageImpl<ArticlesDto> titleSearchPageImpl = new PageImpl<>(titleSearchResult, pageable, titleSearchResult.size());

        // 제목+내용 검색 결과
        List<ArticlesDto> titleContentSearchResult = Arrays.asList(
                new ArticlesDto(1L, "board", "title", "content1", "nickname", 0, 0, 0L, LocalDateTime.now())
        );
        PageImpl<ArticlesDto> titleContentSearchPageImpl = new PageImpl<>(titleContentSearchResult, pageable, titleContentSearchResult.size());

        given(articleRepository.findAllArticles("title", "title", pageable))
                .willReturn(titleSearchPageImpl);
        given(articleRepository.findAllArticles("titleAndContent", "content1", pageable))
                .willReturn(titleContentSearchPageImpl);

        //when
        Page<ArticlesDto> articlesDtosTitle = articleService.findAllArticles(page, "title", "title");
        Page<ArticlesDto> articlesDtosTitleAndContent = articleService.findAllArticles(page, "titleAndContent", "content1");

        //then
        assertThat(articlesDtosTitle.getTotalElements()).isEqualTo(2);
        assertThat(articlesDtosTitleAndContent.getTotalElements()).isEqualTo(1);
    }

}