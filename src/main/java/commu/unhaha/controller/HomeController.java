package commu.unhaha.controller;

import commu.unhaha.domain.User;
import commu.unhaha.dto.SessionUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(@RequestParam(required = false) String result, Model model) {
        model.addAttribute("result", result);
        return "home";
    }
}
