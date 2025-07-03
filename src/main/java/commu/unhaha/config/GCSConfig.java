package commu.unhaha.config;

import org.springframework.beans.factory.annotation.Value;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.Base64;

// 구글 클라우드 스토리지 설정
@Configuration
@ConditionalOnProperty(name = "gcp.enabled", havingValue = "true", matchIfMissing = true)
public class GCSConfig {

    @Value("${gcp.sa-key-base64:}")
    private String gcpKeyBase64;

    @Value("${spring.cloud.gcp.storage.project-id}")
    private String projectId;

    @Bean
    public Storage storage() throws IOException {
        // null 체크 및 로깅 추가
        System.out.println("gcpKeyBase64 length: " + (gcpKeyBase64 != null ? gcpKeyBase64.length() : "null"));
        System.out.println("projectId: " + projectId);
        GoogleCredentials credentials;

        if (!gcpKeyBase64.isEmpty()) {
            // Base64 디코딩 후 사용
            byte[] keyBytes = Base64.getDecoder().decode(gcpKeyBase64);
            try (InputStream keyStream = new ByteArrayInputStream(keyBytes)) {
                credentials = GoogleCredentials.fromStream(keyStream);
            } catch (Exception e) {
                System.err.println("Base64 키 디코딩 실패: " + e.getMessage());
                throw new RuntimeException("GCP 키 디코딩 실패", e);
            }
        } else {
            try {
            // 로컬 개발용 (파일 방식)
            ClassPathResource resource = new ClassPathResource("gcp-key.json");
            if (!resource.exists()) {
                throw new FileNotFoundException("gcp-key.json 파일을 찾을 수 없습니다.");
            }
            credentials = GoogleCredentials.fromStream(resource.getInputStream());
            } catch (Exception e) {
                System.err.println("로컬 키 파일 로드 실패: " + e.getMessage());
                throw new RuntimeException("GCP 키 파일 로드 실패", e);
            }
        }
        // projectId null 체크
        if (projectId == null || projectId.isEmpty()) {
            throw new IllegalArgumentException("Project ID가 설정되지 않았습니다.");
        }

        return StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build()
                .getService();
    }
}
