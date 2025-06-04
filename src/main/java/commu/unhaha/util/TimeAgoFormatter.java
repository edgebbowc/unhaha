package commu.unhaha.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;

public class TimeAgoFormatter {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MM.dd");

    /**
     * createdDate 로부터 now 시각까지의 차이를
     * "N초전", "N분전", "N시간전", "N일전" 또는 "MM.dd" 포맷으로 반환
     */
    public static String format(LocalDateTime createdDate, LocalDateTime now) {
        Period diff = Period.between(createdDate.toLocalDate(), now.toLocalDate());
        Duration timeDiff = Duration.between(createdDate.toLocalTime(), now.toLocalTime());

        // same day → 초/분/시간 전
        if (diff.isZero()) {
            long seconds = timeDiff.getSeconds();
            if (seconds < 60) {
                return seconds + "초전";
            }
            long minutes = timeDiff.toMinutes();
            if (minutes < 60) {
                return minutes + "분전";
            }
            return timeDiff.toHours() + "시간전";
        }

        // 1~7일 전
        int days = diff.getDays();
        if (diff.getYears() == 0 && diff.getMonths() == 0 && days <= 7) {
            return days + "일전";
        }

        // 그 이상 → MM.dd
        return createdDate.format(DATE_FORMATTER);
    }
}
