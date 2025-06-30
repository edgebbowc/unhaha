package commu.unhaha.scheduler;

import commu.unhaha.domain.ArticleImage;
import commu.unhaha.domain.BaseTimeEntity;
import commu.unhaha.domain.CommentImage;
import commu.unhaha.domain.ImageStatus;
import commu.unhaha.file.GCSFileStore;
import commu.unhaha.repository.ArticleImageRepository;
import commu.unhaha.repository.CommentImageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TempImageCleanerTest {

    @InjectMocks
    private TempImageCleanupScheduler tempImageCleanupScheduler;

    @Mock
    private ArticleImageRepository articleImageRepository;

    @Mock
    private CommentImageRepository commentImageRepository;

    @Mock
    private GCSFileStore gcsFileStore;

    @Test
    void 이미지스케줄러테스트() {
        // given
        String articleImageUrl = "https://storage.googleapis.com/test-bucket/article/temp-image.jpg";
        String commentImageUrl = "https://storage.googleapis.com/test-bucket/comment/temp-image2.jpg";
        ArticleImage articleImage = ArticleImage.createTemp(articleImageUrl);
        CommentImage commentImage = CommentImage.createTemp(commentImageUrl);
        setCreatedDateTo(articleImage, LocalDateTime.now().minusHours(2)); // 오래된 이미지
        setCreatedDateTo(commentImage, LocalDateTime.now().minusHours(2)); // 오래된 이미지

        when(articleImageRepository.findByStatusAndCreatedDateBefore(eq(ImageStatus.TEMP), any()))
                .thenReturn(List.of(articleImage));
        when(commentImageRepository.findByStatusAndCreatedDateBefore(eq(ImageStatus.TEMP), any()))
                .thenReturn(List.of(commentImage));

        // GCS 삭제는 성공했다고 가정
        doNothing().when(gcsFileStore).deleteFile(anyString());

        // when
        tempImageCleanupScheduler.cleanUpAllTempImages();

        // then
        // GCS에 삭제 요청이 한 번 갔는지 검증
        verify(gcsFileStore, times(1)).deleteFile(articleImageUrl);
        verify(gcsFileStore, times(1)).deleteFile(commentImageUrl);

        // DB에서 삭제가 요청되었는지 검증
        ArgumentCaptor<List<ArticleImage>> captor = ArgumentCaptor.forClass(List.class);
        verify(articleImageRepository).deleteAll(captor.capture());

        ArgumentCaptor<List<CommentImage>> captor2 = ArgumentCaptor.forClass(List.class);
        verify(commentImageRepository).deleteAll(captor2.capture());

        // AssertJ로 값 검증
        List<ArticleImage> deletedImages = captor.getValue();
        assertThat(deletedImages).hasSize(1);
        assertThat(deletedImages.get(0).getUrl()).isEqualTo(articleImageUrl);
        assertThat(deletedImages.get(0).getStatus()).isEqualTo(ImageStatus.TEMP);

        List<CommentImage> commentDeletedImages = captor2.getValue();
        assertThat(commentDeletedImages).hasSize(1);
        assertThat(commentDeletedImages.get(0).getUrl()).isEqualTo(commentImageUrl);
        assertThat(commentDeletedImages.get(0).getStatus()).isEqualTo(ImageStatus.TEMP);


    }


    // BaseTimeEntity의 createdDate를 강제로 조작하는 유틸
    private void setCreatedDateTo(ArticleImage image, LocalDateTime time) {
        try {
            Field field = BaseTimeEntity.class.getDeclaredField("createdDate");
            field.setAccessible(true);
            field.set(image, time);
        } catch (Exception e) {
            throw new RuntimeException("createdDate 설정 실패", e);
        }
    }
    private void setCreatedDateTo(CommentImage image, LocalDateTime time) {
        try {
            Field field = BaseTimeEntity.class.getDeclaredField("createdDate");
            field.setAccessible(true);
            field.set(image, time);
        } catch (Exception e) {
            throw new RuntimeException("createdDate 설정 실패", e);
        }
    }
}