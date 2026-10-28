package com.curso.instrumento2026.api;

import com.curso.instrumento2026.api.dto.*;
import com.curso.instrumento2026.api.mapper.ClienteMapper;
import com.curso.instrumento2026.domain.*;
import com.curso.instrumento2026.service.ClienteService;
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
@RequestMapping("/api/clientes")
@Tag(name = "Clientes")
public class ClienteController {
    private final ClienteService service;
    private final ClienteMapper mapper;

    public ClienteController(ClienteService service, ClienteMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Operation(summary = "Cadastrar cliente")
    @ApiResponse(responseCode = "201", description = "Cadastro realizado",
            headers = @io.swagger.v3.oas.annotations.headers.Header(name = "Location",
                    description = "URI do recurso criado", schema = @Schema(type = "string", format = "uri")))
    @ApiResponse(responseCode = "400", description = "Dados ou regra de negócio inválidos",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Recurso não encontrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Pessoa já possui cadastro de cliente",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PostMapping
    public ResponseEntity<ClienteResponse> cadastrar(@Valid @RequestBody ClienteRequest request) {
        var cadastrado = service.cadastrar(request.pessoaId(), request.telefone());
        return ResponseEntity.created(URI.create("/api/clientes/" + cadastrado.getId()))
                .body(mapper.toResponse(cadastrado));
    }

    @Operation(summary = "Buscar cliente por ID")
    @ApiResponse(responseCode = "200", description = "Recurso encontrado")
    @ApiResponse(responseCode = "404", description = "Recurso não encontrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/{id}")
    public ClienteResponse buscarPorId(@PathVariable Long id) {
        return mapper.toResponse(service.buscarPorId(id));
    }

    @Operation(summary = "Pesquisar clientes")
    @ApiResponse(responseCode = "200", description = "Pesquisa paginada")
    @ApiResponse(responseCode = "400", description = "Dados ou regra de negócio inválidos",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @GetMapping
    public PaginaResponse<ClienteResponse> pesquisar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String cpf,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return PaginaResponse.de(service.pesquisar(nome, cpf, pageable), mapper::toResponse);
    }

    @Operation(summary = "Atualizar cliente")
    @ApiResponse(responseCode = "200", description = "Cadastro atualizado")
    @ApiResponse(responseCode = "400", description = "Dados ou regra de negócio inválidos",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Recurso não encontrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PutMapping("/{id}")
    public ClienteResponse alterar(@PathVariable Long id, @Valid @RequestBody ClienteAtualizacaoRequest request) {
        return mapper.toResponse(service.alterar(id, request.telefone()));
    }
}
