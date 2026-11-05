package com.curso.instrumento2026.api;

import com.curso.instrumento2026.api.dto.*;
import com.curso.instrumento2026.api.mapper.PessoaMapper;
import com.curso.instrumento2026.domain.*;
import com.curso.instrumento2026.service.PessoaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/api/pessoas")
@Tag(name = "Pessoas")
public class PessoaController {
    private final PessoaService service;
    private final PessoaMapper mapper;

    public PessoaController(PessoaService service, PessoaMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Operation(summary = "Cadastrar pessoa")
    @ApiResponse(responseCode = "201", description = "Cadastro realizado",
            headers = @io.swagger.v3.oas.annotations.headers.Header(name = "Location",
                    description = "URI do recurso criado", schema = @Schema(type = "string", format = "uri")))
    @ApiResponse(responseCode = "400", description = "Dados ou regra de negócio inválidos",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "CPF ou e-mail já cadastrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PostMapping
    public ResponseEntity<PessoaResponse> cadastrar(@Valid @RequestBody PessoaRequest request) {
        var cadastrado = service.cadastrar(mapper.toEntity(request));
        return ResponseEntity.created(URI.create("/api/pessoas/" + cadastrado.getId()))
                .body(mapper.toResponse(cadastrado));
    }

    @Operation(summary = "Buscar pessoa por ID")
    @ApiResponse(responseCode = "200", description = "Recurso encontrado")
    @ApiResponse(responseCode = "404", description = "Recurso não encontrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/{id}")
    public PessoaResponse buscarPorId(@PathVariable Long id) {
        return mapper.toResponse(service.buscarPorId(id));
    }

    @Operation(summary = "Pesquisar pessoas")
    @ApiResponse(responseCode = "200", description = "Pesquisa paginada")
    @ApiResponse(responseCode = "400", description = "Dados ou regra de negócio inválidos",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @GetMapping
    public PaginaResponse<PessoaResponse> pesquisar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) Status status,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return PaginaResponse.de(service.pesquisar(nome, cpf, status, pageable), mapper::toResponse);
    }

    @Operation(summary = "Atualizar pessoa")
    @ApiResponse(responseCode = "200", description = "Cadastro atualizado")
    @ApiResponse(responseCode = "400", description = "Dados ou regra de negócio inválidos",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Recurso não encontrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "CPF ou e-mail já cadastrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PutMapping("/{id}")
    public PessoaResponse alterar(@PathVariable Long id, @Valid @RequestBody PessoaRequest request) {
        return mapper.toResponse(service.alterar(id, request.nome(), request.email(), request.cpf()));
    }

    @Operation(summary = "Alterar status da pessoa")
    @ApiResponse(responseCode = "200", description = "Status atualizado")
    @ApiResponse(responseCode = "400", description = "Dados ou regra de negócio inválidos",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Recurso não encontrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PutMapping("/{id}/status")
    public PessoaResponse alterarStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return mapper.toResponse(service.alterarStatus(id, request.status()));
    }
}
