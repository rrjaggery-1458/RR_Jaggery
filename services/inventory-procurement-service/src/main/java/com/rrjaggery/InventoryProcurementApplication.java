package com.rrjaggery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@EntityScan(basePackages = {
    "com.rrjaggery.inventory.entity",
    "com.rrjaggery.procurement.entity"
})
@EnableJpaRepositories(basePackages = {
    "com.rrjaggery.inventory.repository",
    "com.rrjaggery.procurement.repository"
})
public class InventoryProcurementApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventoryProcurementApplication.class, args);
    }
}
