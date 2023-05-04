package commu.unhaha.domain;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseTimeEntity{

    @Id
    @GeneratedValue
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Embedded
    private UploadFile profileImage;

    public String getRoleKey() {
        return this.role.getKey();
    }

    public User(String name, String nickname, String email, Role role, UploadFile profileImage) {
        this.name = name;
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
