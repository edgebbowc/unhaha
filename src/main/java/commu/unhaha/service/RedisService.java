package commu.unhaha.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class RedisService {

    private Long redisExpireSec = 86400L;

    public void setRedisExpireSec(Long redisExpireSec) {
        this.redisExpireSec = redisExpireSec;
    }

    private final RedisTemplate<String, String> redisTemplate;

    // IP 기반 최초 요청 체크
    public boolean isFirstIpRequest(String clientAddress, Long articleId) {
        String key = generateKey(clientAddress, articleId);

        // setIfAbsent: key가 없으면 "true" 저장 + TTL 설정, 있으면 아무것도 안함
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, "true", redisExpireSec, TimeUnit.SECONDS);

        return Boolean.TRUE.equals(result);
    }

    // 회원 기반 최초 요청 체크
    public boolean isFirstMemberRequest(String email, Long articleId) {
        String key = email;
        String value = articleId.toString();
        // SADD: set에 value 추가, 추가됐으면 1L, 이미 있으면 0L 반환
        Long result = redisTemplate.opsForSet().add(key, value);

        // 최초 추가된 경우에만 만료 시간 설정
        if (result != null && result == 1L) {
            redisTemplate.expire(key, redisExpireSec, TimeUnit.SECONDS);
            return true;
        }
        return false;
    }

    private String generateKey(String clientAddress, Long articleId) {
        return clientAddress + " + " + articleId;
    }



}
