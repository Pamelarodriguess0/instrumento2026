package com.curso.instrumento2026.api;

import com.curso.instrumento2026.api.dto.ApiError;
import com.curso.instrumento2026.api.dto.InstrumentoAtualizacaoRequest;
import com.curso.instrumento2026.api.dto.InstrumentoRequest;
import com.curso.instrumento2026.api.dto.InstrumentoResponse;
import com.curso.instrumento2026.api.dto.MovimentacaoEstoqueRequest;
import com.curso.instrumento2026.api.dto.PaginaResponse;
import com.curso.instrumento2026.api.dto.StatusRequest;
import com.curso.instrumento2026.api.mapper.InstrumentoMapper;
import com.curso.instrumento2026.domain.Instrumento;
import com.curso.instrumento2026.domain.Status;
import com.curso.instrumento2026.service.InstrumentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;
import java.net.URI;
import org.springdoc.core.annotations.ParameterObject;

@RestController
@RequestMapping("/api/instrumentos")
@Tag(
        name = "Instrumentos",
        description = "Operações de cadastro, consulta, atualização, exclusão e controle de estoque de instrumentos musicais"
)
public class InstrumentoController {

    private final InstrumentoService service;
    private final InstrumentoMapper mapper;

    public InstrumentoController(
            InstrumentoService service,
            InstrumentoMapper mapper) {

        this.service = service;
        this.mapper = mapper;
    }

    @Operation(
            summary = "Cadastrar instrumento",
            description = "Cadastra um novo instrumento musical no sistema."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Instrumento cadastrado com sucesso"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos para cadastro",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Categoria ou fornecedor não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
            responseCode = "409",
            description = "Código do instrumento já cadastrado",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @PostMapping
    public ResponseEntity<InstrumentoResponse> cadastrar(
            @Valid @RequestBody InstrumentoRequest request) {

        Instrumento instrumento = mapper.toEntity(request);

        Instrumento cadastrado = service.cadastrar(
                instrumento,
                request.categoriaId(),
                request.fornecedorId()
        );

        URI location =
                URI.create("/api/instrumentos/" + cadastrado.getId());

        return ResponseEntity
                .created(location)
                .body(mapper.toResponse(cadastrado));
    }

    @Operation(
            summary = "Buscar instrumento por ID",
            description = "Consulta os dados de um instrumento musical pelo seu identificador."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Instrumento encontrado com sucesso"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Instrumento não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @GetMapping("/{id}")
    public InstrumentoResponse buscarPorId(
            @PathVariable Long id) {

        return mapper.toResponse(
                service.buscarPorId(id)
        );
    }


    @Operation(
            summary = "Pesquisar instrumentos",
            description = "Pesquisa instrumentos musicais utilizando filtros e paginação."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Pesquisa realizada com sucesso"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Parâmetros de pesquisa ou ordenação inválidos",
            content = @Content(
                    schema = @Schema(implementation = ApiError.class)
            )
    )
    @GetMapping
    public PaginaResponse<InstrumentoResponse> pesquisar(

            @Parameter(
                    description = "Filtra instrumentos pelo nome",
                    example = "Violino"
            )
            @RequestParam(required = false)
            String nomeInstrumento,

            @Parameter(
                    description = "Filtra instrumentos pelo status",
                    example = "ATIVO"
            )
            @RequestParam(required = false)
            Status status,

            @Parameter(
                    description = "Filtra instrumentos pelo identificador da categoria",
                    example = "1"
            )
            @RequestParam(required = false)
            Long categoriaId,

            @Parameter(
                    description = "Filtra instrumentos pelo identificador do fornecedor",
                    example = "1"
            )
            @RequestParam(required = false)
            Long fornecedorId,

            @Parameter(
                    description = "Filtra instrumentos abaixo do estoque mínimo",
                    example = "true"
            )
            @RequestParam(required = false)
            Boolean abaixoEstoqueMinimo,

            @ParameterObject
            @PageableDefault(
                    size = 20,
                    sort = "id",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable) {

        return PaginaResponse.de(
                service.pesquisar(
                        nomeInstrumento,
                        status,
                        categoriaId,
                        fornecedorId,
                        abaixoEstoqueMinimo,
                        pageable
                ),
                mapper::toResponse
        );
    }


    @Operation(
            summary = "Atualizar instrumento",
            description = "Atualiza o nome, preço, estoque mínimo, categoria e fornecedor de um instrumento musical."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Instrumento atualizado com sucesso"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos para atualização do instrumento",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Instrumento, categoria ou fornecedor não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @PutMapping("/{id}")
    public InstrumentoResponse alterar(
            @PathVariable Long id,
            @Valid @RequestBody InstrumentoAtualizacaoRequest request) {

        return mapper.toResponse(
                service.alterar(
                        id,
                        request.nomeInstrumento(),
                        request.precoUnitario(),
                        request.estoqueMinimo(),
                        request.categoriaId(),
                        request.fornecedorId()
                )
        );
    }

    @Operation(
            summary = "Alterar status do instrumento",
            description = "Ativa ou inativa um instrumento musical."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Status do instrumento alterado com sucesso"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Status inválido ou não informado",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Instrumento não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @PutMapping("/{id}/status")
    public InstrumentoResponse alterarStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusRequest request) {

        return mapper.toResponse(
                service.alterarStatus(id, request.status())
        );
    }

    @Operation(
            summary = "Excluir instrumento",
            description = "Exclui um instrumento musical pelo seu identificador."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Instrumento excluído com sucesso",
            content = @Content
    )
    @ApiResponse(
            responseCode = "404",
            description = "Instrumento não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
            responseCode = "409",
            description = "Instrumento vinculado a uma venda; o histórico deve ser preservado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {

        service.excluir(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Registrar entrada de estoque",
            description = "Adiciona uma quantidade ao estoque de um instrumento musical."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Entrada de estoque registrada com sucesso"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Quantidade inválida",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Instrumento não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @PostMapping("/{id}/estoque/entradas")
    public InstrumentoResponse receberEstoque(
            @PathVariable Long id,
            @Valid @RequestBody MovimentacaoEstoqueRequest request) {

        return mapper.toResponse(
                service.receberEstoque(id, request.quantidade())
        );
    }

    @Operation(
            summary = "Registrar saída de estoque",
            description = "Retira uma quantidade do estoque de um instrumento musical."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Saída de estoque registrada com sucesso"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Quantidade inválida ou operação não permitida",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Instrumento não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @PostMapping("/{id}/estoque/saidas")
    public InstrumentoResponse retirarEstoque(
            @PathVariable Long id,
            @Valid @RequestBody MovimentacaoEstoqueRequest request) {

        return mapper.toResponse(
                service.retirarEstoque(id, request.quantidade())
        );
    }
}
