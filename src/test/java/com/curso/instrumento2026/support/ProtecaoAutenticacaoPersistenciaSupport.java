package com.curso.instrumento2026.support;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public abstract class ProtecaoAutenticacaoPersistenciaSupport {
    protected static final Instant INICIO = Instant.parse("2026-11-18T12:00:00Z");
    protected static final Duration PRAZO = Duration.ofMinutes(15);
    @Autowired protected EntityManager em;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired private Environment environment;

    @BeforeEach
    protected void conferirDestinoSeguro() {
        String url = "jdbc:postgresql://127.0.0.1:5432/instrumento2026_test";
        assertTrue(Arrays.asList(environment.getActiveProfiles()).contains("test"));
        assertEquals(url, environment.getProperty("spring.datasource.url"));
        assertEquals(url, environment.getProperty("spring.liquibase.url"));
        assertEquals("public", environment.getProperty("spring.datasource.hikari.schema"));
        assertEquals("public", environment.getProperty("spring.liquibase.default-schema"));
        assertEquals("validate", environment.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("optional:file:./target/aula17-sem-config-externa.properties",
                environment.getProperty("spring.config.import"));
        assertEquals("instrumento2026_test", jdbc.queryForObject("select current_database()", String.class));
        assertEquals("127.0.0.1", jdbc.queryForObject("select host(inet_server_addr())", String.class));
        assertEquals(5432, jdbc.queryForObject("select inet_server_port()", Integer.class));
        assertEquals("public", jdbc.queryForObject("select current_schema()", String.class));
    }

    protected String novaChave() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }
}
