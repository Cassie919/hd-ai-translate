package com.hd.ai.translate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan("com.hd.ai")
@EnableJpaRepositories("com.hd.ai")
@EntityScan("com.hd.ai")
public class AiTranslateApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiTranslateApplication.class, args);
    }
}
