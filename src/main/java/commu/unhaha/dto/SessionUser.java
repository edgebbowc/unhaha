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

    private Long id;
    private String storedImageName;
    private String email;
    private String nickname;

    public SessionUser(User user) {
        this.id =  user.getId();
        this.email = user.getEmail();
        this.nickname = user.getNickname();
        this.storedImageName = user.getProfileImage().getStoreFileUrl();
    }

    public SessionUser(MypageForm mypageForm, SessionUser sessionUser) {
        this.id = sessionUser.getId();
        this.email = mypageForm.getEmail();
        this.nickname = mypageForm.getNickname();
        this.storedImageName = mypageForm.getStoredImageName();
    }
}
