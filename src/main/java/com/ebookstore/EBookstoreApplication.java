package com.ebookstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class EBookstoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(EBookstoreApplication.class, args);
    }
}
