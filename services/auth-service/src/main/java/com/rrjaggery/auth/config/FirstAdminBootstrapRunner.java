package com.rrjaggery.auth.config;

import com.rrjaggery.auth.service.FirstAdminBootstrapService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class FirstAdminBootstrapRunner implements CommandLineRunner {
    private final FirstAdminBootstrapService bootstrapService;
    private final String username;
    private final String password;

    public FirstAdminBootstrapRunner(
            FirstAdminBootstrapService bootstrapService,
            @Value("${bootstrap.admin.username:}") String username,
            @Value("${bootstrap.admin.password:}") String password) {
        this.bootstrapService = bootstrapService;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        bootstrapService.bootstrap(username, password);
    }
}
