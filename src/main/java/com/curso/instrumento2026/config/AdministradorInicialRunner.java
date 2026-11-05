package com.curso.instrumento2026.config;

import com.curso.instrumento2026.service.AdministradorInicialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.security.initial-admin.enabled", havingValue = "true")
public class AdministradorInicialRunner implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdministradorInicialRunner.class);
    private final AdministradorInicialService service;
    private final AdministradorInicialProperties properties;

    public AdministradorInicialRunner(AdministradorInicialService service, AdministradorInicialProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (service.criarSeNecessario(properties).isPresent()) {
            LOGGER.info("Administrador inicial criado. Desative INITIAL_ADMIN_ENABLED após o provisionamento.");
        } else {
            LOGGER.info("Provisionamento do administrador inicial dispensado.");
        }
    }
}
