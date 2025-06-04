package commu.unhaha.repository;

import commu.unhaha.domain.UserLikeComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Arrays;
import java.util.List;

public interface UserLikeCommentRepository extends JpaRepository<UserLikeComment, Long> {
    boolean existsByCommentIdAndUserId(Long commentId, Long userId);

    void deleteByCommentIdAndUserId(Long commentId, Long userId);

    List<UserLikeComment> findByUserId(Long userId);
}
