package com.rrjaggery.common.forms.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.forms", name = "enabled", havingValue = "true")
public class FormDefinitionInitializer implements ApplicationRunner {
    private final FormRegistryService formRegistryService;

    public FormDefinitionInitializer(FormRegistryService formRegistryService) {
        this.formRegistryService = formRegistryService;
    }

    @Override
    public void run(ApplicationArguments args) {
        formRegistryService.ensureOwnedFormsExist();
    }
}
