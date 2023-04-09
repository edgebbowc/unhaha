package commu.unhaha.domain;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

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


    //==비즈니스 로직==//
    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }
    public void changeProfileImage(UploadFile uploadFile) {
        this.profileImage = uploadFile;
    }
}
