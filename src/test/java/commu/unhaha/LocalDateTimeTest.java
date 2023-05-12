package commu.unhaha;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeTest {

    /*
         7일이상 지나면 -> 날짜 출력 ex) 05.20
         1일~7일 -> 1일전
         24시간내 -> 1시간전
    */
    @Test
    public void 게시판날짜테스트() throws Exception{
        //given
        LocalDateTime createdDate = LocalDateTime.of(2023, 5, 4, 22, 35, 1); //2023-05-04 14:30:55
        LocalDateTime now = LocalDateTime.of(2023, 5, 12, 22, 35, 1);
        Period diff = Period.between(createdDate.toLocalDate(), now.toLocalDate());
        Duration timeDiff = Duration.between(createdDate.toLocalTime(), now.toLocalTime());
        String datetime;
        //when
        //7일이상 지나면 -> 날짜 출력 ex) 05.20
        if (diff.getDays() > 7 || diff.getYears() > 0 || diff.getMonths() > 0) {
            datetime = createdDate.format(DateTimeFormatter.ofPattern("MM.dd"));
            System.out.println(datetime);
        }
        // 1일~7일 -> 1일전
        if (diff.getYears() == 0 && diff.getMonths() == 0 && (0 < diff.getDays() && diff.getDays() <= 7)) {
            String days = Integer.toString(diff.getDays());
            datetime = days.concat("일전");
            System.out.println(datetime);
        }
        if (diff.isZero()) {
            System.out.println("같은 날짜");
            if (timeDiff.getSeconds() < 60) {
                String seconds = Long.toString(timeDiff.getSeconds());
                datetime = seconds.concat("초전");
                System.out.println(datetime);
            } else if (timeDiff.toMinutes() < 60) {
                String minutes = Long.toString(timeDiff.toMinutes());
                datetime = minutes.concat("분전");
                System.out.println(datetime);
            } else {
                String hours = Long.toString(timeDiff.toHours());
                datetime = hours.concat("시간전");
                System.out.println(datetime);
            }
        }

    }
}
