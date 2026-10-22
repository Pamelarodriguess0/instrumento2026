
package com.curso.instrumento2026.api;

import com.curso.instrumento2026.api.dto.ApiError;
import com.curso.instrumento2026.api.dto.FornecedorRequest;
import com.curso.instrumento2026.api.dto.FornecedorResponse;
import com.curso.instrumento2026.api.mapper.FornecedorMapper;
import com.curso.instrumento2026.domain.Fornecedor;
import com.curso.instrumento2026.service.FornecedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/fornecedores")
@Tag(
        name = "Fornecedores",
        description = "Operações de cadastro e consulta de fornecedores de instrumentos musicais"
)
public class FornecedorController {

    private final FornecedorService service;
    private final FornecedorMapper mapper;

    public FornecedorController(
            FornecedorService service,
            FornecedorMapper mapper) {

        this.service = service;
        this.mapper = mapper;
    }

    @Operation(
            summary = "Cadastrar fornecedor",
            description = "Cadastra um novo fornecedor de instrumentos musicais."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Fornecedor cadastrado com sucesso"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos para cadastro",
            content = @Content(
                    schema = @Schema(implementation = ApiError.class)
            )
    )
    @ApiResponse(
            responseCode = "409",
            description = "Fornecedor com CNPJ já cadastrado",
            content = @Content(
                    schema = @Schema(implementation = ApiError.class)
            )
    )
    @PostMapping
    public ResponseEntity<FornecedorResponse> cadastrar(
            @Valid @RequestBody FornecedorRequest request) {

        Fornecedor fornecedor = mapper.toEntity(request);

        Fornecedor cadastrado =
                service.cadastrar(fornecedor);

        URI location =
                URI.create("/api/fornecedores/" + cadastrado.getId());

        return ResponseEntity
                .created(location)
                .body(mapper.toResponse(cadastrado));
    }

    @Operation(
            summary = "Buscar fornecedor por ID",
            description = "Consulta os dados de um fornecedor pelo seu identificador."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Fornecedor encontrado com sucesso"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Fornecedor não encontrado",
            content = @Content(
                    schema = @Schema(implementation = ApiError.class)
            )
    )
    @GetMapping("/{id}")
    public FornecedorResponse buscarPorId(
            @PathVariable Long id) {

        return mapper.toResponse(
                service.buscarPorId(id)
        );
    }

    @Operation(
            summary = "Listar fornecedores",
            description = "Lista todos os fornecedores cadastrados no sistema."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Fornecedores listados com sucesso"
    )
    @GetMapping
    public List<FornecedorResponse> listar() {

        return service.listar()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}
