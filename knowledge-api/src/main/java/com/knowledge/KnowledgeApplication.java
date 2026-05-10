package com.knowledge;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.knowledge")
@MapperScan("com.knowledge.mapper")
@ConditionalOnProperty(name = "spring.main.allow-bean-definition-overriding", havingValue = "true")
public class KnowledgeApplication {

    public static void main(String[] args) {
        System.setProperty("spring.main.allow-bean-definition-overriding", "true");
        SpringApplication.run(KnowledgeApplication.class, args);
    }
}