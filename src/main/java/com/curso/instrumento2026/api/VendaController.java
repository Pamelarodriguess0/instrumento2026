package com.curso.instrumento2026.api;

import com.curso.instrumento2026.api.dto.*;
import com.curso.instrumento2026.api.mapper.VendaMapper;
import com.curso.instrumento2026.domain.*;
import com.curso.instrumento2026.service.VendaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/api/vendas")
@Tag(name = "Vendas")
public class VendaController {
    private final VendaService service;
    private final VendaMapper mapper;

    public VendaController(VendaService service, VendaMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Operation(summary = "Cadastrar venda")
    @ApiResponse(responseCode = "201", description = "Cadastro realizado",
            headers = @io.swagger.v3.oas.annotations.headers.Header(name = "Location",
                    description = "URI do recurso criado", schema = @Schema(type = "string", format = "uri")))
    @ApiResponse(responseCode = "400", description = "Dados ou regra de negócio inválidos",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Recurso não encontrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PostMapping
    public ResponseEntity<VendaResponse> cadastrar(@Valid @RequestBody VendaRequest request) {
        var cadastrado = service.cadastrar(request.clienteId(), request.vendedorId());
        return ResponseEntity.created(URI.create("/api/vendas/" + cadastrado.getId()))
                .body(mapper.toResponse(cadastrado));
    }

    @Operation(summary = "Buscar venda por ID")
    @ApiResponse(responseCode = "200", description = "Recurso encontrado")
    @ApiResponse(responseCode = "404", description = "Recurso não encontrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/{id}")
    public VendaResponse buscarPorId(@PathVariable Long id) {
        return mapper.toResponse(service.buscarPorId(id));
    }

    @Operation(summary = "Pesquisar vendas",
            description = "Retorna resumos sem itens ou total. Datas inicial e final inclusivas; período invertido retorna 400. Ordenação por id, dataVenda ou status; página padrão 20, limite 100.")
    @ApiResponse(responseCode = "200", description = "Pesquisa paginada")
    @ApiResponse(responseCode = "400", description = "Dados ou regra de negócio inválidos",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @GetMapping
    public PaginaResponse<VendaResumoResponse> pesquisar(
            @RequestParam(required = false) StatusVenda status,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long vendedorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return PaginaResponse.de(service.pesquisar(status, clienteId, vendedorId, dataInicial, dataFinal, pageable), mapper::toResumo);
    }

    @Operation(summary = "Adicionar instrumento à venda",
            description = "Quantidade em unidades inteiras; o preço atual é preservado no item.")
    @ApiResponse(responseCode = "200", description = "Item adicionado")
    @ApiResponse(responseCode = "400", description = "Dados ou regra de negócio inválidos",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Recurso não encontrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PostMapping("/{id}/itens")
    public VendaResponse adicionarItem(@PathVariable Long id, @Valid @RequestBody ItemVendaRequest request) {
        return mapper.toResponse(service.adicionarItem(id, request.instrumentoId(), request.quantidade()));
    }

    @Operation(summary = "Remover item da venda aberta",
            description = "Não movimenta estoque. Item que não pertence à venda ou venda encerrada retorna 400; venda inexistente retorna 404.")
    @ApiResponse(responseCode = "200", description = "Item removido")
    @ApiResponse(responseCode = "400", description = "Dados ou regra de negócio inválidos",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Recurso não encontrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @DeleteMapping("/{vendaId}/itens/{itemId}")
    public VendaResponse removerItem(@PathVariable Long vendaId, @PathVariable Long itemId) {
        return mapper.toResponse(service.removerItem(vendaId, itemId));
    }

    @Operation(summary = "Finalizar venda e baixar estoque",
            description = "Somente venda aberta e não vazia, com participantes habilitados, instrumentos ativos e saldo suficiente. As baixas são atômicas; falhas preservam os saldos e a venda aberta. Repetir a finalização retorna 400 sem nova baixa.")
    @ApiResponse(responseCode = "200", description = "Venda finalizada")
    @ApiResponse(responseCode = "400", description = "Dados ou regra de negócio inválidos",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Recurso não encontrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PostMapping("/{id}/finalizacao")
    public VendaResponse finalizar(@PathVariable Long id) {
        return mapper.toResponse(service.finalizar(id));
    }

    @Operation(summary = "Cancelar venda aberta",
            description = "Cancela sem movimentar estoque. Venda finalizada ou cancelada retorna 400; não realiza estorno.")
    @ApiResponse(responseCode = "200", description = "Venda cancelada")
    @ApiResponse(responseCode = "400", description = "Dados ou regra de negócio inválidos",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Recurso não encontrado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PostMapping("/{id}/cancelamento")
    public VendaResponse cancelar(@PathVariable Long id) {
        return mapper.toResponse(service.cancelar(id));
    }
}
