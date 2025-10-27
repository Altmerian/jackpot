package com.pshakhlovich.jackpot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.pshakhlovich.jackpot.config.JackpotProperties;

@SpringBootApplication
@EnableConfigurationProperties(JackpotProperties.class)
public class JackpotServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(JackpotServiceApplication.class, args);
	}

}
