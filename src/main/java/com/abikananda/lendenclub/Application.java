package com.abikananda.lendenclub;

import com.abikananda.lendenclub.config.EnvContextInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Application.class);
        app.addInitializers(new EnvContextInitializer());
        app.run(args);
    }
}
