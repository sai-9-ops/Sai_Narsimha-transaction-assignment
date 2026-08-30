package com.example.transactionstarter;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationListener;

@SpringBootApplication
public class TransactionStarterApplication {

    static {
        System.setProperty("org.springframework.boot.logging.LoggingSystem", "none");
        System.setProperty("spring.output.ansi.enabled", "never");
        System.setProperty("spring.main.banner-mode", "off");
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(TransactionStarterApplication.class);
        SpringApplication app = builder.build();
        Collection<ApplicationListener<?>> filtered = app.getListeners().stream()
                .filter(listener -> !(listener instanceof LoggingApplicationListener))
                .collect(Collectors.toList());
        app.setListeners(filtered);
        app.run(args);
    }
}
