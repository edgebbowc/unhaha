package commu.unhaha.service;

import commu.unhaha.domain.UploadFile;
import commu.unhaha.domain.User;
import commu.unhaha.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    //마이페이지 닉네임 변경
    public void editNickname(String email, String formNickname) {
        User user = userRepository.findByEmail(email).orElse(null);
        user.changeNickname(formNickname);

    }
    //마이페이지 프로필사진 변경
    public void editProfileImage(String email, UploadFile profileImage) {
        User user = userRepository.findByEmail(email).orElse(null);
        user.changeProfileImage(profileImage);
    }

    public void deleteUser(String email) {
        userRepository.deleteByEmail(email);
    }

}
