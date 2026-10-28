
package com.curso.instrumento2026.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;
import java.math.BigDecimal;
import java.util.Set;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer quantidadeInteiraDoItemVenda() {
        return openApi -> {
            var item = openApi.getComponents().getSchemas().get("ItemVendaRequest");
            if (item == null) return;
            var quantidade = (io.swagger.v3.oas.models.media.Schema<?>) item.getProperties().get("quantidade");
            // A geração automática reaplica @DecimalMin/@Digits; o domínio exige unidades inteiras.
            quantidade.setType("integer");
            quantidade.setTypes(Set.of("integer"));
            quantidade.setFormat("int32");
            quantidade.setMinimum(BigDecimal.ONE);
            quantidade.setMaximum(BigDecimal.valueOf(Integer.MAX_VALUE));
            quantidade.setMultipleOf(BigDecimal.ONE);
        };
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Controle de Instrumentos Musicais")
                        .description("API para gerenciamento de instrumentos musicais, categorias, fornecedores, estoque, pessoas, clientes, colaboradores e vendas.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Curso de Spring Boot 2026")))
                .externalDocs(new ExternalDocumentation()
                        .description("Material didático do projeto")
                        .url("https://github.com/jeffersonarpasserini/suporteos2026"));
    }
}
