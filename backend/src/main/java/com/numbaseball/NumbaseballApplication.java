package com.numbaseball;

import com.numbaseball.config.GameProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(GameProperties.class)
public class NumbaseballApplication {

	public static void main(String[] args) {
		SpringApplication.run(NumbaseballApplication.class, args);
	}
}
