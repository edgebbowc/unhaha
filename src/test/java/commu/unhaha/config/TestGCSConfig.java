package commu.unhaha.config;

import com.google.cloud.storage.Storage;
import commu.unhaha.file.GCSFileStore;
import commu.unhaha.service.GCSUploader;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.Mockito.when;

@TestConfiguration
@Profile("test")
public class TestGCSConfig {
    @Bean
    @Primary
    public GCSFileStore gcsFileStore() {
        GCSFileStore mock = Mockito.mock(GCSFileStore.class);
        return mock;
    }

    @Bean
    @Primary
    public GCSUploader gcsUploader() {
        return Mockito.mock(GCSUploader.class);
    }

    @Bean
    @Primary
    public Storage storage() {
        return Mockito.mock(Storage.class);
    }
}
