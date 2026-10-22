
package com.curso.instrumento2026.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Controle de Instrumentos Musicais")
                        .description("API para gerenciamento de instrumentos musicais, categorias, fornecedores e estoque.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Curso de Spring Boot 2026")))
                .externalDocs(new ExternalDocumentation()
                        .description("Material didático do projeto")
                        .url("https://github.com/jeffersonarpasserini/suporteos2026"));
    }
}
