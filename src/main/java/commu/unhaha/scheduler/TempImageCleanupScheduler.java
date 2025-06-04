package commu.unhaha.scheduler;

import commu.unhaha.domain.ArticleImage;
import commu.unhaha.domain.CommentImage;
import commu.unhaha.domain.ImageStatus;
import commu.unhaha.file.GCSFileStore;
import commu.unhaha.repository.ArticleImageRepository;
import commu.unhaha.repository.CommentImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class TempImageCleanupScheduler {

    private final CommentImageRepository commentImageRepository;
    private final ArticleImageRepository articleImageRepository;
    private final GCSFileStore gcsFileStore;

    // 1분마다 실행
//    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void cleanUpAllTempImages() {
        log.info("임시 이미지 정리 스케줄러 시작");

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(1);

        // 댓글 이미지 정리
        cleanUpTempCommentImages(cutoff);

        // 게시글 이미지 정리
        cleanUpTempArticleImages(cutoff);

        log.info("임시 이미지 정리 스케줄러 종료");
    }

    private void cleanUpTempCommentImages(LocalDateTime cutoff) {
        List<CommentImage> expiredImages =
                commentImageRepository.findByStatusAndCreatedDateBefore(ImageStatus.TEMP, cutoff);

        if (expiredImages.isEmpty()) {
            log.info("[댓글] 삭제할 TEMP 이미지가 없습니다.");
            return;
        }

        log.info("[댓글] 삭제할 TEMP 이미지 개수: {}", expiredImages.size());
        deleteImagesFromGCS(expiredImages, image -> image.getUrl(), "댓글");
        commentImageRepository.deleteAll(expiredImages);
        log.info("[댓글] TEMP 이미지 정리 완료");
    }

    private void cleanUpTempArticleImages(LocalDateTime cutoff) {
        List<ArticleImage> expiredImages =
                articleImageRepository.findByStatusAndCreatedDateBefore(ImageStatus.TEMP, cutoff);

        if (expiredImages.isEmpty()) {
            log.info("[게시글] 삭제할 TEMP 이미지가 없습니다.");
            return;
        }

        log.info("[게시글] 삭제할 TEMP 이미지 개수: {}", expiredImages.size());
        deleteImagesFromGCS(expiredImages, image -> image.getUrl(), "게시글");
        articleImageRepository.deleteAll(expiredImages);
        log.info("[게시글] TEMP 이미지 정리 완료");
    }

    private <T> void deleteImagesFromGCS(List<T> images, Function<T, String> urlExtractor, String type) {
        int successCount = 0;
        int errorCount = 0;

        for (T image : images) {
            try {
                String url = urlExtractor.apply(image);
                gcsFileStore.deleteFile(url);
                successCount++;
            } catch (Exception e) {
                log.warn("[{}] GCS 이미지 삭제 실패: {}", type, e.getMessage());
                errorCount++;
            }
        }

        log.info("[{}] GCS 이미지 삭제 결과 - 성공: {}, 실패: {}", type, successCount, errorCount);
    }
}
