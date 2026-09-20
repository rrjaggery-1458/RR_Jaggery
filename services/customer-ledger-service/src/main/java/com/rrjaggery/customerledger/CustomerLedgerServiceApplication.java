package com.rrjaggery.customerledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.rrjaggery.customerledger", "com.rrjaggery.common"})
public class CustomerLedgerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CustomerLedgerServiceApplication.class, args);
    }
}
