package commu.unhaha.dto;

import commu.unhaha.domain.UploadFile;
import lombok.Data;
import org.hibernate.validator.constraints.Range;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class MypageForm {

    private MultipartFile userImage;

    private String storedImageName;

    @NotNull
    private String email;

    @Pattern(regexp = "[ㄱ-ㅎ|가-힣|a-z|A-Z|0-9]{3,13}", message = "3글자 이상 입력해주세요 특수문자 및 공백은 불가능합니다")
    private String nickname;
}
