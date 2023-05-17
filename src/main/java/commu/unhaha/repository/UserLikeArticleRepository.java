package commu.unhaha.repository;

import commu.unhaha.domain.UserLikeArticle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLikeArticleRepository extends JpaRepository<UserLikeArticle, Long> {

    boolean existsByArticleIdAndUserId(Long articleId, Long userId);

    void deleteByArticleIdAndUserId(Long articleId, Long userId);
}
