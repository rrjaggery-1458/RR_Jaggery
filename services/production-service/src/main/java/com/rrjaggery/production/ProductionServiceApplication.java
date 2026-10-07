package com.rrjaggery.production;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import com.rrjaggery.common.forms.EnableInternalForms;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@ComponentScan(basePackages = {"com.rrjaggery.production", "com.rrjaggery.common"})
@EnableInternalForms
@EntityScan(basePackages = {"com.rrjaggery.production.entity", "com.rrjaggery.common.forms.entity"})
@EnableJpaRepositories(basePackages = {"com.rrjaggery.production.repository", "com.rrjaggery.common.forms.repository"})
public class ProductionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductionServiceApplication.class, args);
    }
}
