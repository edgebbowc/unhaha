package commu.unhaha.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true) // user 회원탈퇴시 게시판글도 삭제
    private List<Article> articles = new ArrayList<>();

    @Embedded
    private UploadFile profileImage;

    public String getRoleKey() {
        return this.role.getKey();
    }

    @Builder
    public User(String nickname, String email, Role role, UploadFile profileImage) {
        this.nickname = nickname;
        this.email = email;
        this.role = role;
        this.profileImage = profileImage;
    }

    //==비즈니스 로직==//
    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }
    public void changeProfileImage(UploadFile uploadFile) {
        this.profileImage = uploadFile;
    }
}
