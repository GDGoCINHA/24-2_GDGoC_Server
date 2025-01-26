package inha.gdgoc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class GdgocApplication {

	public static void main(String[] args) {
		SpringApplication.run(GdgocApplication.class, args);
	}

}
