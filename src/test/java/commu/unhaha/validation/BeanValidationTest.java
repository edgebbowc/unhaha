package commu.unhaha.validation;

import commu.unhaha.dto.MypageForm;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class BeanValidationTest {

    @Test
    public void 닉네임검증() throws Exception {
        //given
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        MypageForm lessThan3AndIncludeSpecial = new MypageForm();
        lessThan3AndIncludeSpecial.setEmail("dddd@dddd");
        lessThan3AndIncludeSpecial.setNickname("1!");

        MypageForm includeSpecial = new MypageForm();
        includeSpecial.setEmail("dddd@dddd");
        includeSpecial.setNickname("12!");

        MypageForm space = new MypageForm();
        space.setEmail("dddd@dddd");
        space.setNickname(" 12 3");

        //when
        Set<ConstraintViolation<MypageForm>> validate_lessThan3AndIncludeSpecial = validator.validate(lessThan3AndIncludeSpecial);
        Set<ConstraintViolation<MypageForm>> validate_includeSpecial = validator.validate(includeSpecial);
        Set<ConstraintViolation<MypageForm>> validate_space = validator.validate(space);

        //then
        String message_lessThan3 = getMessage(validate_lessThan3AndIncludeSpecial);
        String message_special = getMessage(validate_includeSpecial);
        String message_space = getMessage(validate_space);

        Assertions.assertThat(message_lessThan3).isEqualTo("3글자 이상 입력해주세요 특수문자 및 공백은 불가능합니다");
        Assertions.assertThat(message_special).isEqualTo("3글자 이상 입력해주세요 특수문자 및 공백은 불가능합니다");
        Assertions.assertThat(message_space).isEqualTo("3글자 이상 입력해주세요 특수문자 및 공백은 불가능합니다");
    }

    private String getMessage(Set<ConstraintViolation<MypageForm>> violations) {
        Iterator<ConstraintViolation<MypageForm>> iterator = violations.iterator();
        String message = "";
        while (iterator.hasNext()) {
            ConstraintViolation<MypageForm> next = iterator.next();
            message = next.getMessage();
        }
        return message;
    }
}
