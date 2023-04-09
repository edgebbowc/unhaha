package commu.unhaha.controller;

import commu.unhaha.domain.UploadFile;
import commu.unhaha.domain.User;
import commu.unhaha.dto.MypageForm;
import commu.unhaha.dto.SessionUser;
import commu.unhaha.file.FileStore;
import commu.unhaha.repository.UserRepository;
import commu.unhaha.service.UserService;
import commu.unhaha.validation.NicknameValidator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.io.IOException;

@Controller
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final NicknameValidator nicknameValidator;
    private final FileStore fileStore;

    @GetMapping("/mypage")
    public String Profile(@SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser, Model model) {

        model.addAttribute("user", loginUser);

        return "mypage";
    }

    @PostMapping("/mypage")
    public String editProfile(@Validated @ModelAttribute("user") MypageForm mypageForm, BindingResult bindingResult,
                              @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser,
                              HttpSession session, Model model, RedirectAttributes rttr) throws IOException {
        // Bean Validation 적용
        if (bindingResult.hasErrors()) {
            log.info("errors={}", bindingResult);
            mypageForm.setStoredImageName(loginUser.getStoredImageName());
            model.addAttribute("msg", "3글자 이상 입력해주세요 특수문자 및 공백은 불가능합니다");
            return "mypage";
        }
        // 원래 닉네임과 같고 원래 프로필 이미지와 같을 경우
        if ((mypageForm.getNickname().equals(loginUser.getNickname())) && (mypageForm.getUserImage().isEmpty())) {
            rttr.addFlashAttribute("msg", "회원정보를 변경하였습니다");
            return "redirect:/mypage";
        }
        // 원래 닉네임과 같고 프로필 이미지만 변경할 경우
        if ((mypageForm.getNickname().equals(loginUser.getNickname())) && (!mypageForm.getUserImage().isEmpty())) {
            editProfileImage(mypageForm, loginUser);
            setSession(mypageForm, session);
            rttr.addFlashAttribute("msg", "회원정보를 변경하였습니다");
            return "redirect:/mypage";
        }

        // 닉네임 중복 검증
        nicknameValidator.validate(mypageForm, bindingResult);
        if (bindingResult.hasErrors()) {
            mypageForm.setStoredImageName(loginUser.getStoredImageName());
            model.addAttribute("msg", "이미 사용중인 닉네임입니다");
            return "mypage";
        }

        // 닉네임만 변경할 경우
        if (mypageForm.getUserImage().isEmpty()) {
            userService.editNickname(mypageForm.getEmail(), mypageForm.getNickname());
            mypageForm.setStoredImageName(loginUser.getStoredImageName());
            setSession(mypageForm, session);
            rttr.addFlashAttribute("msg", "회원정보를 변경하였습니다");
            return "redirect:/mypage";
        }

        // 닉네임과 프로필 이미지 둘 다 변경
        editProfileImage(mypageForm, loginUser);
        userService.editNickname(mypageForm.getEmail(), mypageForm.getNickname());
        setSession(mypageForm, session);
        rttr.addFlashAttribute("msg", "회원정보를 변경하였습니다");
        return "redirect:/mypage";
    }










    private void editProfileImage(MypageForm mypageForm, SessionUser loginUser) throws IOException {
        fileStore.deleteFile(loginUser.getStoredImageName());
        UploadFile uploadFile = fileStore.storeFile(mypageForm.getUserImage());
        mypageForm.setStoredImageName(uploadFile.getStoreFileName());
        userService.editProfileImage(mypageForm.getEmail(), uploadFile);
    }

    private void setSession(MypageForm mypageForm, HttpSession session) {
        SessionUser sessionUser = new SessionUser(mypageForm);
        session.setAttribute(SessionConst.LOGIN_USER, sessionUser);
    }

}
