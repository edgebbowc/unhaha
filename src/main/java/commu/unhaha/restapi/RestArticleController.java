package commu.unhaha.restapi;

import commu.unhaha.dto.ArticlesDto;
import commu.unhaha.file.FileStore;
import commu.unhaha.repository.ArticleRepository;
import commu.unhaha.repository.UserRepository;
import commu.unhaha.service.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Iterator;

@RestController
@RequiredArgsConstructor
@Slf4j
public class RestArticleController {
    private final ArticleService articleService;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final FileStore fileStore;

    @GetMapping(value = "/api/new")
    public Page<ArticlesDto> allArticles(@RequestParam(value = "page", defaultValue = "1") int page,
                                         String searchType, String keyword) {
        int jpaPage = page - 1;
        if (keyword == null) {
            Page<ArticlesDto> articles = articleService.pageList(jpaPage); //페이징
//            int totalPages = articles.getTotalPages();
//            int maxPage = 5; //페이지 1~5, 6~10
//            int start = (articles.getNumber() / maxPage) * maxPage + 1; //start = 1, 6, 11
//            int end = totalPages == 0 ? 1 : (start + (maxPage - 1) < totalPages ? start + (maxPage - 1) : totalPages); //end= 5, 10, 15
//            Iterator<ArticlesDto> iterator = articles.iterator();
//            while (iterator.hasNext()) {
//                ArticlesDto articleDto = iterator.next();
//                Document doc = Jsoup.parse(articleDto.getContent());
//                if (doc.selectFirst("img") != null) {
//                    String src = doc.selectFirst("img").attr("src");
//                    articleDto.setThumb(src);
//                }
//                LocalDateTime createdDate = articleDto.getCreatedDate();
//                LocalDateTime now = LocalDateTime.now();
//                String dateTime = articleService.calDateTime(createdDate, now);
//                articleDto.setDateTime(dateTime);
//            }
            return articles;

        } else {
            Page<ArticlesDto> articles = articleService.searchPageList(jpaPage, keyword, searchType); //제목, 제목+내용 검색 페이징
//            int totalPages = articles.getTotalPages();
//            int maxPage = 5; //페이지 1~5, 6~10
//            int start = (articles.getNumber() / maxPage) * maxPage + 1; //start = 1, 6, 11
//            int end = totalPages == 0 ? 1 : (start + (maxPage - 1) < totalPages ? start + (maxPage - 1) : totalPages); //end= 5, 10, 15
            Iterator<ArticlesDto> iterator = articles.iterator();
            while (iterator.hasNext()) {
                ArticlesDto articleDto = iterator.next();
                Document doc = Jsoup.parse(articleDto.getContent());
                if (doc.selectFirst("img") != null) {
                    String src = doc.selectFirst("img").attr("src");
                    articleDto.setThumb(src);
                }
                LocalDateTime createdDate = articleDto.getCreatedDate();
                LocalDateTime now = LocalDateTime.now();
                String dateTime = articleService.calDateTime(createdDate, now);
                articleDto.setDateTime(dateTime);
            }

            return articles;
        }


    }
}
