package commu.unhaha.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class RedisService {

    private final Long redisExpireSec = 86400L;
    private final RedisTemplate<String, Boolean> boolRedisTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    public boolean isFirstIpRequest(String clientAddress, Long articleId) {
        String key = generateKey(clientAddress, articleId);
        log.info("user article request key: {}", key);
        if (boolRedisTemplate.hasKey(key)) {
            return false;
        }
        return true;
    }

    public void writeClientRequest(String clientAddress, Long articleId) {
        String key = generateKey(clientAddress, articleId);

        // 사실 set 할때 value가 필요없음. 그나마 가장 작은 불린으로 넣긴 했는데 아직 레디스를 잘 몰라서 이렇게 쓰고 있음
        boolRedisTemplate.opsForValue().set(key, true);
        boolRedisTemplate.expire(key, redisExpireSec, TimeUnit.SECONDS);
    }

    private String generateKey(String clientAddress, Long articleId) {
        return clientAddress + " + " + articleId;
    }

    public void setValuesList(String key, String data) {
        redisTemplate.opsForList().rightPushAll(key, data);
    }

    public List<String> getValuesList(String key) {
        Long len = redisTemplate.opsForList().size(key);
        return len == 0 ? new ArrayList<>() : redisTemplate.opsForList().range(key, 0, len-1);
    }

}
