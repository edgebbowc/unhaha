package commu.unhaha;


import commu.unhaha.config.TestGCSConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestGCSConfig.class)
class UnhahaApplicationTests {

	@Test
	void contextLoads() {

	}

}
