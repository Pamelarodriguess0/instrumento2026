
package com.curso.instrumento2026.api;

import com.curso.instrumento2026.api.dto.ApiError;
import com.curso.instrumento2026.api.dto.CategoriaInstrumentoAtualizacaoRequest;
import com.curso.instrumento2026.api.dto.CategoriaInstrumentoRequest;
import com.curso.instrumento2026.api.dto.CategoriaInstrumentoResponse;
import com.curso.instrumento2026.api.dto.PaginaResponse;
import com.curso.instrumento2026.api.dto.StatusRequest;
import com.curso.instrumento2026.api.mapper.CategoriaInstrumentoMapper;
import com.curso.instrumento2026.domain.CategoriaInstrumento;
import com.curso.instrumento2026.domain.Status;
import com.curso.instrumento2026.service.CategoriaInstrumentoService;
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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;

@RestController
@RequestMapping("/api/categorias")
@Tag(
        name = "Categorias",
        description = "Operações de cadastro, consulta, atualização, exclusão e alteração de status das categorias de instrumentos musicais"
)
public class CategoriaInstrumentoController {

    private final CategoriaInstrumentoService service;
    private final CategoriaInstrumentoMapper mapper;

    public CategoriaInstrumentoController(
            CategoriaInstrumentoService service,
            CategoriaInstrumentoMapper mapper) {

        this.service = service;
        this.mapper = mapper;
    }

    @Operation(
            summary = "Cadastrar categoria",
            description = "Cadastra uma nova categoria de instrumentos musicais."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Categoria cadastrada com sucesso"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos para cadastro",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
            responseCode = "409",
            description = "Nome da categoria já cadastrado",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @PostMapping
    public ResponseEntity<CategoriaInstrumentoResponse> cadastrar(
            @Valid @RequestBody CategoriaInstrumentoRequest request) {

        CategoriaInstrumento categoria = mapper.toEntity(request);

        CategoriaInstrumento cadastrada = service.cadastrar(categoria);

        URI location =
                URI.create("/api/categorias/" + cadastrada.getId());

        return ResponseEntity
                .created(location)
                .body(mapper.toResponse(cadastrada));
    }

    @Operation(
            summary = "Buscar categoria por ID",
            description = "Consulta uma categoria pelo seu identificador."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Categoria encontrada com sucesso"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Categoria não encontrada",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @GetMapping("/{id}")
    public CategoriaInstrumentoResponse buscarPorId(
            @PathVariable Long id) {

        return mapper.toResponse(
                service.buscarPorId(id)
        );
    }



    @Operation(
            summary = "Pesquisar categorias",
            description = "Pesquisa categorias utilizando filtros de nome, status e paginação."
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
    @Parameters({
            @Parameter(
                    name = "page",
                    description = "Número da página, começando em zero",
                    example = "0",
                    in = ParameterIn.QUERY
            ),
            @Parameter(
                    name = "size",
                    description = "Quantidade de registros por página",
                    example = "20",
                    in = ParameterIn.QUERY
            ),
            @Parameter(
                    name = "sort",
                    description = "Campo e direção da ordenação",
                    example = "id,asc",
                    in = ParameterIn.QUERY
            )
    })
    @GetMapping
    public PaginaResponse<CategoriaInstrumentoResponse> pesquisar(

            @Parameter(
                    description = "Filtra categorias pelo nome",
                    example = "Cordas"
            )
            @RequestParam(required = false)
            String nome,

            @Parameter(
                    description = "Filtra categorias pelo status",
                    example = "ATIVO"
            )
            @RequestParam(required = false)
            Status status,

            @Parameter(hidden = true)
            @PageableDefault(
                    size = 20,
                    sort = "id",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable) {

        return PaginaResponse.de(
                service.pesquisar(nome, status, pageable),
                mapper::toResponse
        );
    }



    @Operation(
            summary = "Atualizar categoria",
            description = "Atualiza o nome de uma categoria de instrumentos musicais."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Categoria atualizada com sucesso"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos para atualização",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Categoria não encontrada",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
            responseCode = "409",
            description = "Nome da categoria já cadastrado",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @PutMapping("/{id}")
    public CategoriaInstrumentoResponse alterar(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaInstrumentoAtualizacaoRequest request) {

        return mapper.toResponse(
                service.alterar(id, request.nome())
        );
    }

    @Operation(
            summary = "Alterar status da categoria",
            description = "Ativa ou inativa uma categoria de instrumentos musicais."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Status da categoria alterado com sucesso"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Status inválido ou não informado",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Categoria não encontrada",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @PutMapping("/{id}/status")
    public CategoriaInstrumentoResponse alterarStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusRequest request) {

        return mapper.toResponse(
                service.alterarStatus(id, request.status())
        );
    }

    @Operation(
            summary = "Excluir categoria",
            description = "Exclui uma categoria que não possui instrumentos vinculados."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Categoria excluída com sucesso",
            content = @Content
    )
    @ApiResponse(
            responseCode = "404",
            description = "Categoria não encontrada",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @ApiResponse(
            responseCode = "409",
            description = "Categoria não pode ser excluída porque possui instrumentos vinculados",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {

        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
