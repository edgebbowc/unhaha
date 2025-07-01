package commu.unhaha.config;

import commu.unhaha.domain.Article;
import commu.unhaha.domain.Role;
import commu.unhaha.domain.UploadFile;
import commu.unhaha.domain.User;
import commu.unhaha.repository.ArticleRepository;
import commu.unhaha.repository.UserRepository;
import commu.unhaha.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

//@Component
//@RequiredArgsConstructor
//@Profile("!test")  // test 프로파일에서는 실행하지 않음
//public class DataInitializer {
//    private final UserRepository userRepository;
//    private final ArticleRepository articleRepository;
//    private final CommentService commentService;
//
//    /**
//     * 테스트용 데이터 추가
//     */
//    @PostConstruct
//    public void init() {
//        User user = userRepository.save(new User("길동", "홍길동", "222@naver.com", Role.USER, new UploadFile("userImage", "userImage")));
//        User user2 = userRepository.save(new User("로니콜먼", "로니콜먼", "333@naver.com", Role.USER, new UploadFile("userImage", "userImage")));
//        for (int i = 1; i <= 100; i++) {
//            articleRepository.save(new Article("보디빌딩", "오운완" + i, "오늘도 운동 완료", user, 0, 0));
//        }
//        for (int i = 400; i < 501; i++) {
//            articleRepository.save(new Article("파워리프팅", "3대 " + i, "오늘도 운동 완료", user, 0, 0));
//        }
//        for (int i = 1; i <= 100; i++) {
//            articleRepository.save(new Article("크로스핏", "와드 " + i, "오늘도 운동 완료", user, 0, 0));
//        }
//        commentService.createComment(user2, 100L, "Light Weight BABY!", null,null);
//        commentService.createComment(user2, 100L, "Easy Weight BABY!", null,1L);
//    }
//}
