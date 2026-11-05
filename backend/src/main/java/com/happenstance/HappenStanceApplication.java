
package com.happenstance;

import com.happenstance.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class HappenStanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HappenStanceApplication.class, args);
    }
}
