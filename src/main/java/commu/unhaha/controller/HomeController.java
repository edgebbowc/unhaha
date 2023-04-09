package commu.unhaha.controller;

import commu.unhaha.domain.User;
import commu.unhaha.dto.SessionUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {

//        //세션에 회원데이터가 없으면 home
//        if (loginUser == null) {
//            return "home";
//        }
//
//        //세션이 유지되면 로그인으로 이동
//        model.addAttribute("user", loginUser);
//        return "loginHome";
//        if (loginUser != null) {
//            model.addAttribute("user", loginUser);
//        }
        return "home";
    }
}
