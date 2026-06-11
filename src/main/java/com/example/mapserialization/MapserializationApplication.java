package com.example.mapserialization;

import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRedisDocumentRepositories
public class MapserializationApplication {

	public static void main(String[] args) {
		SpringApplication.run(MapserializationApplication.class, args);
	}

}
