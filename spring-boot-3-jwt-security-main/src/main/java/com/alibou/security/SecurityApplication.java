package com.alibou.security;

import com.alibou.security.auth.AuthenticationService;
import com.alibou.security.auth.RegisterRequest;
import com.alibou.security.user.Role;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import static com.alibou.security.user.Role.*;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class SecurityApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecurityApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(AuthenticationService service) {
		return args -> {
			try {
				var admin = RegisterRequest.builder()
						.firstname("Admin")
						.lastname("Admin")
						.email("admin@mail.com")
						.password("password")
						.role(ADMIN)
						.build();

				System.out.println("Admin token: " + service.register(admin).getAccessToken());

			} catch (Exception e) {
				System.out.println("Admin already exists, skipping creation.");
			}
			
			try {
				var ai = RegisterRequest.builder()
						.firstname("Agent")
						.lastname("Copilot IA")
						.email("ai@sagemcom.com")
						.password("aipassword")
						.role(ADMIN)
						.build();
				service.register(ai);
				System.out.println("AI Agent created.");
			} catch (Exception e) {
				System.out.println("AI Agent already exists, skipping creation.");
			}
		};
	}
}
