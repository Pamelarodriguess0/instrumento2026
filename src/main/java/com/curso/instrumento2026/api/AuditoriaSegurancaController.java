package com.curso.instrumento2026.api;

import com.curso.instrumento2026.api.dto.EventoSegurancaResponse;
import com.curso.instrumento2026.domain.TipoEventoSeguranca;
import com.curso.instrumento2026.service.AuditoriaSegurancaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auditoria-seguranca")
public class AuditoriaSegurancaController {
    private final AuditoriaSegurancaService auditoria;

    public AuditoriaSegurancaController(AuditoriaSegurancaService auditoria) {
        this.auditoria = auditoria;
    }

    @Operation(summary = "Consultar eventos de segurança",
            description = "Retorna os eventos mais recentes primeiro. A resposta inclui os eventos em content "
                    + "e os metadados em page. Os eventos publicados omitem o hash de correlação.",
            parameters = {
                @Parameter(name = "page", in = ParameterIn.QUERY, description = "Página a partir de zero",
                        schema = @Schema(type = "integer", minimum = "0", defaultValue = "0")),
                @Parameter(name = "size", in = ParameterIn.QUERY, description = "Eventos por página",
                        schema = @Schema(type = "integer", minimum = "1", defaultValue = "20"))
            })
    @GetMapping
    public PagedModel<EventoSegurancaResponse> consultar(
            @Parameter(description = "Filtra pelo tipo de evento") @RequestParam(required = false) TipoEventoSeguranca tipo,
            @Parameter(hidden = true) @PageableDefault(size = 20) Pageable pageable) {
        return new PagedModel<>(auditoria.consultar(tipo, pageable).map(EventoSegurancaResponse::de));
    }
}
