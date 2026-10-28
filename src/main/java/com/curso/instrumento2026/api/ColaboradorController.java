package com.curso.instrumento2026.api;

import com.curso.instrumento2026.api.dto.*;
import com.curso.instrumento2026.api.mapper.ColaboradorMapper;
import com.curso.instrumento2026.domain.*;
import com.curso.instrumento2026.service.ColaboradorService;
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
@RequestMapping("/api/colaboradores")
@Tag(name = "Colaboradores")
public class ColaboradorController {
    private final ColaboradorService service;
    private final ColaboradorMapper mapper;

    public ColaboradorController(ColaboradorService service, ColaboradorMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Operation(summary = "Cadastrar colaborador")
    @ApiResponse(responseCode = "201", description = "Cadastro realizado",
            headers = @io.swagger.v3.oas.annotations.headers.Header(name = "Location",
                    description = "URI do recurso criado", schema = @Schema(type = "string", format = "uri")))
    @ApiResponse(responseCode = "400", description = "Dados ou regra de negócio inválidos",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Recurso não encontrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Pessoa já possui vínculo ou matrícula já cadastrada",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PostMapping
    public ResponseEntity<ColaboradorResponse> cadastrar(@Valid @RequestBody ColaboradorRequest request) {
        var cadastrado = service.cadastrar(request.pessoaId(), request.matricula(), request.funcao(), request.dataAdmissao());
        return ResponseEntity.created(URI.create("/api/colaboradores/" + cadastrado.getId()))
                .body(mapper.toResponse(cadastrado));
    }

    @Operation(summary = "Buscar colaborador por ID")
    @ApiResponse(responseCode = "200", description = "Recurso encontrado")
    @ApiResponse(responseCode = "404", description = "Recurso não encontrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/{id}")
    public ColaboradorResponse buscarPorId(@PathVariable Long id) {
        return mapper.toResponse(service.buscarPorId(id));
    }

    @Operation(summary = "Pesquisar colaboradores")
    @ApiResponse(responseCode = "200", description = "Pesquisa paginada")
    @ApiResponse(responseCode = "400", description = "Dados ou regra de negócio inválidos",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @GetMapping
    public PaginaResponse<ColaboradorResponse> pesquisar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) FuncaoColaborador funcao,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return PaginaResponse.de(service.pesquisar(nome, matricula, funcao, pageable), mapper::toResponse);
    }

    @Operation(summary = "Atualizar colaborador")
    @ApiResponse(responseCode = "200", description = "Cadastro atualizado")
    @ApiResponse(responseCode = "400", description = "Dados ou regra de negócio inválidos",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Recurso não encontrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Pessoa já possui vínculo ou matrícula já cadastrada",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PutMapping("/{id}")
    public ColaboradorResponse alterar(@PathVariable Long id, @Valid @RequestBody ColaboradorAtualizacaoRequest request) {
        return mapper.toResponse(service.alterar(id, request.matricula(), request.funcao()));
    }
}
