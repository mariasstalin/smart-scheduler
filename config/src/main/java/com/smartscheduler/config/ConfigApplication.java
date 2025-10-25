
package com.smartscheduler.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.scheduling.annotation.EnableScheduling; // Required for @Scheduled

@SpringBootApplication
@EnableConfigServer
@EnableScheduling
public class ConfigApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigApplication.class, args);
    }
}
