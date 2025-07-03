package commu.unhaha.dto;

import commu.unhaha.domain.Role;
import commu.unhaha.domain.UploadFile;
import commu.unhaha.domain.User;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Getter
@Builder
@Slf4j
public class OAuthAttributes {

    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private UploadFile profileImage;
    private String email;
    private String nickname;
    private String accessToken;

    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {

        return ofNaver("id", attributes);
    }

    private static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        return OAuthAttributes.builder()
                .email((String) response.get("email"))
                .nickname((String) response.get("nickname"))
                .attributes(response)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }


    public User toEntity() {

        return User.builder()
                .nickname(nickname)
                .email(email)
                .role(Role.USER)
                .profileImage(new UploadFile("userImage", "userImage"))
                .build();
    }
}
