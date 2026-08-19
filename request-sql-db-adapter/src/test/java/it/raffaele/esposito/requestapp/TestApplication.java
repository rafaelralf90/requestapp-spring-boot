package it.raffaele.esposito.requestapp;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.time.Clock;

@SpringBootApplication
@ComponentScan("it.raffaele.esposito.requestapp.adapter.out.repository.sqldb")
public class TestApplication {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
