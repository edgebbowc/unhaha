package commu.unhaha.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PageInfo {
    int start;
    int end;
    int maxPage;
    int totalPages;
    int currentPage; // 현재 페이지 추가
}
