package commu.unhaha.repository;

import commu.unhaha.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByArticleIdOrderByCreatedDateAsc(Long articleId, Pageable pageable);
}
