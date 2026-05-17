package com.fco.platform;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FcoPlatformApplication {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
        dotenv.entries()
                .forEach(entry ->
                        System.setProperty(entry.getKey(), entry.getValue()));
        SpringApplication.run(FcoPlatformApplication.class, args);
    }
}
