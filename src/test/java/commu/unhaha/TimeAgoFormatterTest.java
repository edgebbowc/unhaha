package commu.unhaha;

import commu.unhaha.util.TimeAgoFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TimeAgoFormatterTest {

    @Test
    @DisplayName("30초 전 테스트")
    void testSecondsAgo() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime created = now.minusSeconds(30);

        String result = TimeAgoFormatter.format(created, now);
        assertEquals("30초전", result);
    }

    @Test
    @DisplayName("동일한 시간 (0초 차이)")
    void testSameTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime created = now;

        String result = TimeAgoFormatter.format(created, now);
        assertEquals("1초전", result);
    }

    @Test
    @DisplayName("미래 시간 (음수 차이)")
    void testFutureTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime created = now.plusSeconds(10);

        String result = TimeAgoFormatter.format(created, now);
        assertEquals("1초전", result);
    }

    @Test
    @DisplayName("10분 전 테스트")
    void testMinutesAgo() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime created = now.minusMinutes(10);

        String result = TimeAgoFormatter.format(created, now);
        assertEquals("10분전", result);
    }

    @Test
    @DisplayName("5시간 전 테스트")
    void testHoursAgo() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime created = now.minusHours(5);

        String result = TimeAgoFormatter.format(created, now);
        assertEquals("5시간전", result);
    }

    @Test
    @DisplayName("3일 전 테스트")
    void testDaysAgo() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime created = now.minusDays(3);

        String result = TimeAgoFormatter.format(created, now);
        assertEquals("3일전", result);
    }

    @Test
    @DisplayName("7일 초과 - 날짜 형식 반환")
    void testMoreThanSevenDays() {
        LocalDateTime now = LocalDateTime.of(2023, 12, 20, 10, 0);
        LocalDateTime created = LocalDateTime.of(2023, 12, 5, 10, 0);

        String result = TimeAgoFormatter.format(created, now);
        assertEquals("12.05", result);
    }

    @Test
    @DisplayName("경계값: 정확히 60초")
    void testExactly60Seconds() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime created = now.minusSeconds(60);

        String result = TimeAgoFormatter.format(created, now);
        assertEquals("1분전", result);
    }

    @Test
    @DisplayName("경계값: 정확히 60분")
    void testExactly60Minutes() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime created = now.minusMinutes(60);

        String result = TimeAgoFormatter.format(created, now);
        assertEquals("1시간전", result);
    }

    @Test
    @DisplayName("경계값: 정확히 24시간")
    void testExactly24Hours() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime created = now.minusHours(24);

        String result = TimeAgoFormatter.format(created, now);
        assertEquals("1일전", result);
    }

    @Test
    @DisplayName("경계값: 정확히 7일")
    void testExactly7Days() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime created = now.minusDays(7);

        String result = TimeAgoFormatter.format(created, now);
        assertEquals("7일전", result);
    }

    @Test
    @DisplayName("경계값: 8일 (7일 초과)")
    void testMoreThan7Days() {
        LocalDateTime now = LocalDateTime.of(2023, 12, 20, 10, 0);
        LocalDateTime created = LocalDateTime.of(2023, 12, 12, 10, 0);

        String result = TimeAgoFormatter.format(created, now);
        assertEquals("12.12", result);
    }

    @Test
    @DisplayName("59초 테스트")
    void test59Seconds() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime created = now.minusSeconds(59);

        String result = TimeAgoFormatter.format(created, now);
        assertEquals("59초전", result);
    }

    @Test
    @DisplayName("59분 테스트")
    void test59Minutes() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime created = now.minusMinutes(59);

        String result = TimeAgoFormatter.format(created, now);
        assertEquals("59분전", result);
    }

    @Test
    @DisplayName("23시간 테스트")
    void test23Hours() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime created = now.minusHours(23);

        String result = TimeAgoFormatter.format(created, now);
        assertEquals("23시간전", result);
    }

    @Test
    @DisplayName("연도가 다른 경우")
    void testDifferentYear() {
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 10, 0);
        LocalDateTime created = LocalDateTime.of(2023, 12, 25, 10, 0);

        String result = TimeAgoFormatter.format(created, now);
        assertEquals("12.25", result);
    }

    @Test
    @DisplayName("월이 넘어가는 경우")
    void testMonthBoundary() {
        LocalDateTime now = LocalDateTime.of(2023, 12, 5, 10, 0);
        LocalDateTime created = LocalDateTime.of(2023, 11, 20, 10, 0);

        String result = TimeAgoFormatter.format(created, now);
        assertEquals("11.20", result);
    }
}
