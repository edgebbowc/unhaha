package commu.unhaha.scheduler;

import commu.unhaha.domain.ArticleImage;
import commu.unhaha.domain.ImageStatus;
import commu.unhaha.file.GCSFileStore;
import commu.unhaha.repository.ArticleImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArticleImageCleaner {
    private final ArticleImageRepository articleImageRepository;
    private final GCSFileStore gcsFileStore;

    // 매 1시간마다 실행 (예: 1:00, 2:00, 3:00...)
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void cleanUpTempImages() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(1);

        List<ArticleImage> expiredTempImages =
                articleImageRepository.findByStatusAndCreatedDateBefore(ImageStatus.TEMP, cutoff);

        if (expiredTempImages.isEmpty()) {
            log.info("삭제할 TEMP 이미지가 없습니다.");
            return;
        }

        log.info("삭제할 TEMP 이미지 개수: {}", expiredTempImages.size());

        for (ArticleImage image : expiredTempImages) {
            try {
                gcsFileStore.deleteFile(image.getUrl());
            } catch (Exception e) {
                log.warn("GCS 이미지 삭제 실패 (스케줄러): {}", image.getUrl(), e);
            }
        }

        articleImageRepository.deleteAll(expiredTempImages);
        log.info("TEMP 이미지 정리 완료");
    }
}

