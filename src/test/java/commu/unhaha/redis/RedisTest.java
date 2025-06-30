package commu.unhaha.redis;

import commu.unhaha.service.RedisService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class RedisTest {

    @Autowired
    RedisService redisService;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.0.0-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void setRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redisContainer::getHost);
        registry.add("spring.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @Test
    @DisplayName("레디스 연결 테스트")
    void redisConnectionTest() {
        final String key = "a";
        final String data = "1";

        final ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(key, data);

        final String s = valueOperations.get(key);
        Assertions.assertThat(s).isEqualTo(data);
    }

    @Test
    @DisplayName("레디스 TTL 테스트")
    void testIpRequestTtlExpiration() throws InterruptedException {
        String clientAddress = "192.168.0.1";
        Long articleId = 123L;
        String key = clientAddress + " + " + articleId;
        redisService.setRedisExpireSec(3L);

        // 1. 최초 요청: true 반환
        assertTrue(redisService.isFirstIpRequest(clientAddress, articleId));
        assertTrue(Boolean.TRUE.equals(redisTemplate.hasKey(key)));

        // 2. TTL이 지나기 전: false 반환
        assertFalse(redisService.isFirstIpRequest(clientAddress, articleId));

        // 3. TTL만큼 대기 (예: 3초)
        Thread.sleep(3100);

        // 4. TTL 만료 후: key가 삭제됨
        assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(key)));

        // 5. 다시 요청: true 반환 (다시 저장됨)
        assertTrue(redisService.isFirstIpRequest(clientAddress, articleId));
    }
}
