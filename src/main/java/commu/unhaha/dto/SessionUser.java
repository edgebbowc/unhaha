package commu.unhaha.dto;

import commu.unhaha.domain.UploadFile;
import commu.unhaha.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class SessionUser implements Serializable {

    private String storedImageName;
    private String email;
    private String nickname;
    private String accessToken;

    public SessionUser(User user, String accessToken) {
        this.email = user.getEmail();
        this.nickname = user.getNickname();
        this.storedImageName = user.getProfileImage().getStoreFileName();
        this.accessToken = accessToken;
    }

    public SessionUser(MypageForm mypageForm, String accessToken) {
        this.email = mypageForm.getEmail();
        this.nickname = mypageForm.getNickname();
        this.storedImageName = mypageForm.getStoredImageName();
        this.accessToken = accessToken;
    }
}
