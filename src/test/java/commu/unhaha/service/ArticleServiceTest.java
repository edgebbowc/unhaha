package commu.unhaha.service;

import commu.unhaha.domain.*;
import commu.unhaha.dto.ArticlesDto;
import commu.unhaha.file.GCSFileStore;
import commu.unhaha.repository.ArticleImageRepository;
import commu.unhaha.repository.ArticleRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
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
    public void pageList() throws Exception {
        //given
        User user = new User("name", "nickname", "email", Role.USER, new UploadFile("userimage", "userimage"));
        Article article1 = new Article("board", "title", "content", user, 0, 0);
        Article article2 = new Article("board", "title", "content", user, 0, 0);
        ReflectionTestUtils.setField(article1, "id", 1L);
        ReflectionTestUtils.setField(article2, "id", 2L);

        int page = 0;
        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "id"));
        List<Article> articleList = new ArrayList<>();
        articleList.add(article1);
        articleList.add(article2);

        PageImpl<Article> articlePageImpl = new PageImpl<>(articleList, pageable, articleList.size());
        given(articleRepository.findAll(pageable)).willReturn(articlePageImpl);

        //when
        Page<ArticlesDto> articlesDtos = articleService.pageList(page);

        //then
        assertThat(articlesDtos.getTotalElements()).isEqualTo(2);
        assertThat(articlesDtos.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "id"));
    }

    @DisplayName("검색조건O 페이징")
    @Test
    public void searchPageList() throws Exception {
        //given
        User user = new User("name", "nickname", "email", Role.USER, new UploadFile("userimage", "userimage"));
        Article article1 = new Article("board", "title", "content1", user, 0, 0);
        Article article2 = new Article("board", "title", "content2", user, 0, 0);
        ReflectionTestUtils.setField(article1, "id", 1L);
        ReflectionTestUtils.setField(article2, "id", 2L);

        int page = 0;
        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "id"));
        List<Article> articleListTitle = new ArrayList<>();
        List<Article> articleListTitleAndContent = new ArrayList<>();
        articleListTitle.add(article1);
        articleListTitle.add(article2);
        articleListTitleAndContent.add(article1);

        PageImpl<Article> articlePageTitleImpl = new PageImpl<>(articleListTitle, pageable, articleListTitle.size());
        PageImpl<Article> articlePageTitleContentImpl = new PageImpl<>(articleListTitleAndContent, pageable, articleListTitleAndContent.size());
        given(articleRepository.findByTitleContaining("title", pageable)).willReturn(articlePageTitleImpl);
        given(articleRepository.findByTitleContainingOrContentContaining("content1", "content1", pageable)).willReturn(articlePageTitleContentImpl);

        //when
        Page<ArticlesDto> articlesDtosTitle = articleService.searchPageList(page, "title", "title");
        Page<ArticlesDto> articlesDtosTitleAndContent = articleService.searchPageList(page, "content1", "titleAndContent");

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