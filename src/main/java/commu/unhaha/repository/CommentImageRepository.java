package commu.unhaha.repository;

import commu.unhaha.domain.ArticleImage;
import commu.unhaha.domain.CommentImage;
import commu.unhaha.domain.ImageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommentImageRepository extends JpaRepository<CommentImage, Long> {
    // TEMP 상태이면서 cutoff 시간 이전에 생성된 이미지 조회 (스케줄러용)
    List<CommentImage> findByStatusAndCreatedDateBefore(ImageStatus status, LocalDateTime cutoff);

    // 특정 게시글에 연결된 이미지들 조회
    List<CommentImage> findByCommentId(Long commentId);

    List<CommentImage> findByUrlInAndStatus(List<String> imageUrls, ImageStatus status);

    Optional<CommentImage> findByUrlAndStatus(String url, ImageStatus status);

    @Query("SELECT ci.url FROM CommentImage ci WHERE ci.comment.id IN :commentIds")
    List<String> findUrlsByCommentIdIn(@Param("commentIds") Collection<Long> commentIds);

}
