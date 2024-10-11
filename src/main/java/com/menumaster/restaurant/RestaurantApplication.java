package com.menumaster.restaurant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class RestaurantApplication {

	@RestController
	class HelloworldController {
		@GetMapping("/")
		String home() {
		return "home!";
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(RestaurantApplication.class, args);
	}

}
