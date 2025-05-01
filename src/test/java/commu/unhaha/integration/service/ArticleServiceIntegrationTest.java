package commu.unhaha.integration.service;

import commu.unhaha.domain.Article;
import commu.unhaha.domain.Role;
import commu.unhaha.domain.UploadFile;
import commu.unhaha.domain.User;
import commu.unhaha.repository.ArticleRepository;
import commu.unhaha.repository.UserRepository;
import commu.unhaha.service.ArticleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public class ArticleServiceIntegrationTest {
    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.0.0-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void setRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redisContainer::getHost);
        registry.add("spring.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @Test
    @DisplayName("게시글 조회 동시성 테스트")
    void ArticleConcurrencyTest() throws Exception {
        User user = userRepository.save(new User("동시성", "동시성", "222@naver.com", Role.USER, new UploadFile("userImage", "userImage")));
        Article article = Article.builder()
                .board("보디빌딩")
                .title("동시성 테스트")
                .content("테스트")
                .user(user)
                .viewCount(0)
                .likeCount(0)
                .build();

        articleRepository.save(article);
        Long articleId = article.getId();

        int userCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(userCount);

        // when
        for (int i = 0; i < userCount; i++) {
            String userId = String.valueOf(i);
            executorService.execute(() -> {
                try {

                    articleService.noneMemberView(articleId, userId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 요청이 끝날 때까지 대기

        // then
        Article findArticle = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalStateException("게시글 없음"));
        assertEquals(userCount, findArticle.getViewCount());
    }
}

