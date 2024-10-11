package com.menumaster.restaurant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class RestaurantApplication {

	@RequestMapping("/home")
	public String home() {
		return "Spring Boot Application for restaurant Carinho";
	}

	public static void main(String[] args) {
		SpringApplication.run(RestaurantApplication.class, args);
	}

}
