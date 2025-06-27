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
        Duration totalDuration = Duration.between(createdDate, now);
        long totalSeconds = totalDuration.getSeconds();

        // 음수 처리 (미래 시간이거나 거의 동시인 경우)
        if (totalSeconds <= 0) {
            return "1초전";
        }

        // 60초 미만
        if (totalSeconds < 60) {
            return totalSeconds + "초전";
        }

        // 60분 미만
        long minutes = totalSeconds / 60;
        if (minutes < 60) {
            return minutes + "분전";
        }

        // 24시간 미만
        long hours = totalSeconds / 3600;
        if (hours < 24) {
            return hours + "시간전";
        }

        // 7일 미만
        long days = totalSeconds / (3600 * 24);
        if (days <= 7) {
            return days + "일전";
        }

        // 7일 이상 - 날짜 출력
        return createdDate.format(DATE_FORMATTER);
    }
}
