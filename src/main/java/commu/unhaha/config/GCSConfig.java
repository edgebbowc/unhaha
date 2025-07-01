package commu.unhaha.config;

import com.google.api.client.util.Value;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

// 구글 클라우드 스토리지 설정
@Configuration
public class GCSConfig {

    @Value("${GCP_SA_KEY_BASE64:}")
    private String gcpKeyBase64;

    @Value("${spring.cloud.gcp.storage.project-id}")
    private String projectId;

    @Bean
    public Storage storage() throws IOException {
        GoogleCredentials credentials;

        if (!gcpKeyBase64.isEmpty()) {
            // Base64 디코딩 후 사용
            byte[] keyBytes = Base64.getDecoder().decode(gcpKeyBase64);
            try (InputStream keyStream = new ByteArrayInputStream(keyBytes)) {
                credentials = GoogleCredentials.fromStream(keyStream);
            }
        } else {
            // 로컬 개발용 (파일 방식)
            ClassPathResource resource = new ClassPathResource("gcp-key.json");
            credentials = GoogleCredentials.fromStream(resource.getInputStream());
        }

        return StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build()
                .getService();
    }
}
