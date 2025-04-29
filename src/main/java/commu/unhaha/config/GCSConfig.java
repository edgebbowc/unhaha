package commu.unhaha.config;

import com.google.api.client.util.Value;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;

// 구글 클라우드 스토리지 설정
@Configuration
public class GCSConfig {

    @Value("${spring.cloud.gcp.storage.project-id}")
    private String projectId;

    @Bean
    public Storage storage() throws IOException {
        ClassPathResource resource = new ClassPathResource("gcp-key.json");
        GoogleCredentials credentials = GoogleCredentials.fromStream(resource.getInputStream());

        return StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build()
                .getService();
    }
}
