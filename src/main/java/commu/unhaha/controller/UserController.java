package commu.unhaha.controller;

import commu.unhaha.domain.UploadFile;
import commu.unhaha.dto.MypageForm;
import commu.unhaha.dto.SessionUser;
import commu.unhaha.file.GCSFileStore;
import commu.unhaha.repository.UserRepository;
import commu.unhaha.service.UserService;
import commu.unhaha.validation.NicknameValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final NicknameValidator nicknameValidator;
    private final GCSFileStore gcsFileStore;

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

    // 프로필 이미지 저장
    @PostMapping("/mypage/upload-profile-image")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadProfileImage(
            @RequestParam("image") MultipartFile image,
            @SessionAttribute(name = SessionConst.LOGIN_USER, required = false) SessionUser loginUser,
            HttpSession session) throws IOException {

        if (image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false));
        }

        gcsFileStore.deleteFile(loginUser.getStoredImageName()); // 기존 이미지 삭제
        UploadFile uploadFile = gcsFileStore.storeProfileImage(image);   // GCS에 새 이미지 업로드

        // DB 반영
        userService.editProfileImage(loginUser.getEmail(), uploadFile);

        // 세션 업데이트
        loginUser.setStoredImageName(uploadFile.getStoreFileUrl());
        session.setAttribute(SessionConst.LOGIN_USER, loginUser);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "imageUrl", uploadFile.getStoreFileUrl()
        ));
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
    /** 프로필 이미지 변경 */
    private void editProfileImage(MypageForm mypageForm, SessionUser loginUser) throws IOException {
        gcsFileStore.deleteFile(loginUser.getStoredImageName());
        UploadFile uploadFile = gcsFileStore.storeProfileImage(mypageForm.getUserImage());
        mypageForm.setStoredImageName(uploadFile.getStoreFileUrl());
        userService.editProfileImage(mypageForm.getEmail(), uploadFile);
    }

    private void setSession(MypageForm mypageForm, SessionUser loginUser, HttpSession session) {
        SessionUser sessionUser = new SessionUser(mypageForm, loginUser);
        session.setAttribute(SessionConst.LOGIN_USER, sessionUser);
    }

}
