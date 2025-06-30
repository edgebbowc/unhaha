package commu.unhaha.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GCSUploader {

    private final Storage storage;

    @Value("${spring.cloud.gcp.storage.bucket}")
    private String bucketName;

    public String upload(MultipartFile file, String dirName) throws IOException {
        String fileName = dirName + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();

        storage.create(blobInfo, file.getBytes());

        // GCS 기본 URL
        return String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);
    }

    //  이미지 삭제
    public void deleteByUrl(String url) {
        String objectName = extractObjectNameFromUrl(url);

        if (objectName == null || objectName.isEmpty()) {
            throw new IllegalArgumentException("GCS URL에서 objectName 추출 실패: " + url);
        }

        boolean deleted = storage.delete(bucketName, objectName);

        if (!deleted) {
            throw new IllegalStateException("GCS에서 파일 삭제 실패: " + objectName);
        }
    }

    //  유틸: GCS URL → object name 추출
    private String extractObjectNameFromUrl(String url) {
        String base = "https://storage.googleapis.com/" + bucketName + "/";
        if (!url.startsWith(base)) return null;
        return url.substring(base.length());
    }
}
