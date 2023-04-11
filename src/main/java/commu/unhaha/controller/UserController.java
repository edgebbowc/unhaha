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
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

@Controller
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
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
            setSession(mypageForm, loginUser, session);
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
            setSession(mypageForm, loginUser, session);
            rttr.addFlashAttribute("msg", "회원정보를 변경하였습니다");
            return "redirect:/mypage";
        }

        // 닉네임과 프로필 이미지 둘 다 변경
        editProfileImage(mypageForm, loginUser);
        userService.editNickname(mypageForm.getEmail(), mypageForm.getNickname());
        setSession(mypageForm, loginUser, session);
        rttr.addFlashAttribute("msg", "회원정보를 변경하였습니다");
        return "redirect:/mypage";
    }

    @GetMapping("/mypage/withdraw")
    public String withDraw(@SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser,
                           RedirectAttributes rttr, HttpSession session) throws IOException {
        String accessToken = loginUser.getAccessToken();
        String deleteUrl = "https://nid.naver.com/oauth2.0/token?grant_type=delete&client_id="+ System.getenv("client-id") +"&client_secret=" + System.getenv("client-secret") + "&access_token=" + accessToken +"&service_provider=NAVER";
        log.info("deleteUrl={}", deleteUrl);
        try {
            userService.deleteUser(loginUser.getEmail());
            String result = requestToServer(deleteUrl);
            rttr.addAttribute("result", result);
            session.invalidate();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "redirect:/";
    }

    private String requestToServer(String deleteUrl) throws IOException {
        URL url = new URL(deleteUrl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        int responseCode = con.getResponseCode(); //헤더 필드 읽을때 connect()를 호출하지 않고 암시적으로 연결이 설정된다.
        String result;
        if (responseCode == 200) {
            result = "회원 탈퇴에 성공했습니다";
        } else {
            result = "회원 탈퇴에 실패했습니다";
        }
        con.disconnect();
        return result;

    }

    private void editProfileImage(MypageForm mypageForm, SessionUser loginUser) throws IOException {
        fileStore.deleteFile(loginUser.getStoredImageName());
        UploadFile uploadFile = fileStore.storeFile(mypageForm.getUserImage());
        mypageForm.setStoredImageName(uploadFile.getStoreFileName());
        userService.editProfileImage(mypageForm.getEmail(), uploadFile);
    }

    private void setSession(MypageForm mypageForm, SessionUser loginUser, HttpSession session) {
        SessionUser sessionUser = new SessionUser(mypageForm, loginUser.getAccessToken());
        session.setAttribute(SessionConst.LOGIN_USER, sessionUser);
    }

}
