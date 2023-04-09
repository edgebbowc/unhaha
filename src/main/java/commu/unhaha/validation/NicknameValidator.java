package commu.unhaha.validation;

import commu.unhaha.dto.MypageForm;
import commu.unhaha.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@RequiredArgsConstructor
public class NicknameValidator implements Validator {

    private final UserRepository userRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return MypageForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        MypageForm mypageForm = (MypageForm) target;
        if (userRepository.existsByNickname(mypageForm.getNickname())) {
            errors.rejectValue("nickname", "닉네임 중복", "이미 사용중인 닉네임입니다");
        }
    }
}
