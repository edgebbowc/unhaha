package commu.unhaha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class UnhahaApplication {

	public static void main(String[] args) {
		SpringApplication.run(UnhahaApplication.class, args);
	}

}
