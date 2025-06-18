package commu.unhaha.unit.service;

import commu.unhaha.domain.*;
import commu.unhaha.dto.ArticlesDto;
import commu.unhaha.dto.WriteArticleForm;
import commu.unhaha.file.GCSFileStore;
import commu.unhaha.repository.ArticleImageRepository;
import commu.unhaha.repository.ArticleRepository;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
    private GCSFileStore gcsFileStore;

    @InjectMocks
    private ArticleService articleService;

    private User user;
    private Article article;

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
        ReflectionTestUtils.setField(article, "id", 1L);
    }
    @DisplayName("게시판 수정")
    @Test
    public void editArticle() throws Exception {
        //given
        User user = new User("name", "nickname", "email", Role.USER, new UploadFile("userimage", "userimage"));
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
        articleService.editArticle(articleId, form);

        // then
        assertThat(tempImage.getStatus()).isEqualTo(ImageStatus.ACTIVE);
    }

    @DisplayName("게시판 삭제")
    @Test
    public void deleteArticle() throws Exception {
        // given
        Long articleId = 1L;
        List<ArticleImage> mockImages = List.of(
                ArticleImage.createTemp("url1"),
                ArticleImage.createTemp("url2")
        );

        // validateArticle() 정상 통과하게 모킹
        when(articleRepository.existsById(articleId)).thenReturn(true);

        // findByArticleId() 모킹
        when(articleImageRepository.findByArticleId(articleId)).thenReturn(mockImages);

        // gcsFileStore.deleteFile() 모킹
        doNothing().when(gcsFileStore).deleteFile(anyString());

        // when
        articleService.deleteArticle(articleId);

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

    @DisplayName("검색조건X 페이징")
    @Test
    public void findAllArticlesNotSearch() throws Exception {
        //given
        User user = new User("name", "nickname", "email", Role.USER, new UploadFile("userimage", "userimage"));
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
        User user = new User("name", "nickname", "email", Role.USER, new UploadFile("userimage", "userimage"));

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

    @Test
    void addViewCount() {
    }

    @Test
    void noneMemberView() {
    }

    @Test
    void memberView() {
    }

    @Test
    void calDateTime() {
    }

    @Test
    void findLike() {
    }

    @Test
    void saveLike() {
    }
}