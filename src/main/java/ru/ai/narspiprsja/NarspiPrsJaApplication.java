package ru.ai.narspiprsja;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NarspiPrsJaApplication {

    public static void main(String[] args) {
        SpringApplication.run(NarspiPrsJaApplication.class, args);
    }

}
