package org.api.standingsapiservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
@EnableCaching
public class StandingsApiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StandingsApiServiceApplication.class, args);
    }

}
