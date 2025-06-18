package commu.unhaha.repository;

import commu.unhaha.domain.Article;
import commu.unhaha.domain.ArticleImage;
import commu.unhaha.domain.ImageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ArticleImageRepository extends JpaRepository<ArticleImage, Long> {
    // TEMP 상태이면서 cutoff 시간 이전에 생성된 이미지 조회 (스케줄러용)
    List<ArticleImage> findByStatusAndCreatedDateBefore(ImageStatus status, LocalDateTime cutoff);

    // 글 내용에서 추출한 이미지 URL 목록으로 해당 이미지들 조회
    List<ArticleImage> findByUrlIn(Collection<String> urls);

    // 특정 게시글에 연결된 이미지들 조회
    List<ArticleImage> findByArticleId(Long articleId);

    List<ArticleImage> findByUrlInAndStatus(List<String> urls, ImageStatus status);
}
