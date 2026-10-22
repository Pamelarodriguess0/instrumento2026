
package com.curso.instrumento2026.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(
        name = "Infraestrutura",
        description = "Endpoints para verificação da disponibilidade da API"
)
public class HealthController {

    @Operation(
            summary = "Verificar saúde da API",
            description = "Retorna OK quando a aplicação está respondendo."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Aplicação respondendo normalmente"
    )
    @GetMapping("/api/health")
    public String health() {
        return "OK";
    }
}
