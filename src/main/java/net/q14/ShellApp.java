package net.q14;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.jline.PromptProvider;

import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;



@SpringBootApplication
public class ShellApp {

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext context = SpringApplication.run(ShellApp.class, args);
    }

    @Bean
    public PromptProvider myPromptProvider() {
        return () -> new AttributedString("migrate:>", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
    }



}


