package com.rrjaggery.customerledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@ComponentScan(basePackages = {"com.rrjaggery.customerledger", "com.rrjaggery.common"})
public class CustomerLedgerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CustomerLedgerServiceApplication.class, args);
    }
}
