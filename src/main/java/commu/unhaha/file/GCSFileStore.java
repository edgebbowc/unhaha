package commu.unhaha.file;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import commu.unhaha.domain.UploadFile;
import commu.unhaha.service.GCSUploader;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GCSFileStore {

    private final GCSUploader gcsUploader;
    private final Storage storage;

    @Value("${spring.cloud.gcp.storage.bucket}")
    private String bucketName;

    /** 프로필 이미지 저장 */
    public UploadFile storeProfileImage(MultipartFile multipartFile) throws IOException {
        return storeImage(multipartFile, "profile");
    }

    /** 게시글 이미지 저장 */
    public UploadFile storeArticleImage(MultipartFile multipartFile) throws IOException {
        return storeImage(multipartFile, "article");
    }


    private UploadFile storeImage(MultipartFile multipartFile, String folderName) throws IOException {
        if (multipartFile.isEmpty()) {
            return null;
        }

        String originalFilename = Optional.ofNullable(multipartFile.getOriginalFilename())
                .orElse("unknown");
        String gcsUrl = gcsUploader.upload(multipartFile, folderName);

        return new UploadFile(originalFilename, gcsUrl);
    }

    /** 공통 삭제 */
    public void deleteFile(String url) {
        if (url == null || url.isBlank()) return;

        // 기본 이미지 예외 처리
        if (url.equals("userImage")) return;

        gcsUploader.deleteByUrl(url);
    }


}

