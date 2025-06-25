package commu.unhaha.repository;

import commu.unhaha.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByArticleIdAndParentIsNullOrderByCreatedDateAsc(Long articleId, Pageable pageable); // 1) 페이징된 루트 댓글

    List<Comment> findByArticleIdAndParentIsNotNullOrderByCreatedDateAsc(Long articleId); // N+1문제 터짐

    long countByArticleIdAndParentIsNull(Long articleId);

    long countByArticleIdAndParentIsNullAndCreatedDateBefore(Long articleId, LocalDateTime createdDate);

    /**
     * 댓글 페이징 계산
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.article.id = :articleId " +
            "AND c.parent IS NULL AND c.id < :commentId")
    long countByArticleIdAndParentIsNullAndIdLessThan(
            @Param("articleId") Long articleId,
            @Param("commentId") Long commentId);

    /**
     * 대댓글들만 fetch join으로 조회
     */
    @Query("SELECT c FROM Comment c " +
            "LEFT JOIN FETCH c.user u " +
            "LEFT JOIN FETCH c.parent p " +
            "LEFT JOIN FETCH p.user pu " +
            "LEFT JOIN FETCH c.article a " +
            "LEFT JOIN FETCH a.user au " +
            "WHERE c.article.id = :articleId AND c.parent IS NOT NULL " +
            "ORDER BY c.createdDate ASC")
    List<Comment> findRepliesWithUserByArticleId(@Param("articleId") Long articleId); // 3) 이 루트 댓글들에 딸린 모든 자식 댓글(깊이 무관)을 시간순으로 한 번에 조회

    /**
     * 댓글 작성 시 부모 댓글을 연관 관계와 함께 조회
     */
    @Query(
            "SELECT c FROM Comment c " +
                    "LEFT JOIN FETCH c.parent p " +
                    "LEFT JOIN FETCH c.user u " +
                    "LEFT JOIN FETCH c.article a " +
                    "LEFT JOIN FETCH a.user au " +
                    "WHERE c.id = :parentId"
    )
    Optional<Comment> findParentWithRelations(@Param("parentId") Long parentId);

    @Query("SELECT c FROM Comment c " +
            "JOIN FETCH c.user " +
            "JOIN FETCH c.article " +
            "WHERE c.user.email = :email " +
            "ORDER BY c.createdDate DESC")
    List<Comment> findByUserEmailOrderByCreatedDateDesc(@Param("email") String email);

    @Query("SELECT c FROM Comment c " +
            "JOIN FETCH c.user " +
            "JOIN FETCH c.article " +
            "JOIN UserLikeComment ulc ON c.id = ulc.comment.id " +
            "JOIN User u ON ulc.user.id = u.id " +
            "WHERE u.email = :email " +
            "ORDER BY ulc.createdDate DESC")
    List<Comment> findLikedCommentsByUserEmail(@Param("email") String email);
}
