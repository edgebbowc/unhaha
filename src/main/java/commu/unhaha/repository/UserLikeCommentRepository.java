package commu.unhaha.repository;

import commu.unhaha.domain.UserLikeComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLikeCommentRepository extends JpaRepository<UserLikeComment, Long> {
    boolean existsByCommentIdAndUserId(Long articleId, Long userId);

    void deleteByCommentIdAndUserId(Long articleId, Long userId);
}
