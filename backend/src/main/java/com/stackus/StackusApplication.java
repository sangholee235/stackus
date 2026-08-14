package com.stackus;

import com.stackus.config.GameProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(GameProperties.class)
public class StackusApplication {

	public static void main(String[] args) {
		SpringApplication.run(StackusApplication.class, args);
	}
}
