package com.curso.instrumento2026.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;

@Configuration
public class PasswordEncoderConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        PasswordEncoder delegate = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return delegate.encode(rawPassword);
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                // BCrypt pode ignorar bytes excedentes na verificação; rejeitar antes da delegação.
                return rawPassword != null
                        && rawPassword.toString().getBytes(StandardCharsets.UTF_8).length <= 72
                        && delegate.matches(rawPassword, encodedPassword);
            }

            @Override
            public boolean upgradeEncoding(String encodedPassword) {
                return delegate.upgradeEncoding(encodedPassword);
            }
        };
    }
}
