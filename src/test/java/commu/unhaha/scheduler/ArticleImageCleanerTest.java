package commu.unhaha.scheduler;

import commu.unhaha.domain.ArticleImage;
import commu.unhaha.domain.BaseTimeEntity;
import commu.unhaha.domain.ImageStatus;
import commu.unhaha.file.GCSFileStore;
import commu.unhaha.repository.ArticleImageRepository;
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
class ArticleImageCleanerTest {

    @InjectMocks
    private ArticleImageCleaner articleImageCleaner;

    @Mock
    private ArticleImageRepository articleImageRepository;

    @Mock
    private GCSFileStore gcsFileStore;

    @Test
    void 이미지스케줄러테스트() {
        // given
        String imageUrl = "https://storage.googleapis.com/test-bucket/article/temp-image.jpg";
        ArticleImage tempImage = ArticleImage.createTemp(imageUrl);
        setCreatedDateTo(tempImage, LocalDateTime.now().minusHours(2)); // 오래된 이미지

        when(articleImageRepository.findByStatusAndCreatedDateBefore(eq(ImageStatus.TEMP), any()))
                .thenReturn(List.of(tempImage));

        // GCS 삭제는 성공했다고 가정
        doNothing().when(gcsFileStore).deleteFile(anyString());

        // when
        articleImageCleaner.cleanUpTempImages();

        // then
        // GCS에 삭제 요청이 한 번 갔는지 검증
        verify(gcsFileStore, times(1)).deleteFile(imageUrl);

        // DB에서 삭제가 요청되었는지 검증
        ArgumentCaptor<List<ArticleImage>> captor = ArgumentCaptor.forClass(List.class);
        verify(articleImageRepository).deleteAll(captor.capture());

        // AssertJ로 값 검증
        List<ArticleImage> deletedImages = captor.getValue();
        assertThat(deletedImages).hasSize(1);
        assertThat(deletedImages.get(0).getUrl()).isEqualTo(imageUrl);
        assertThat(deletedImages.get(0).getStatus()).isEqualTo(ImageStatus.TEMP);
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
}