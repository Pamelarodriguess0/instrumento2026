package com.curso.instrumento2026.api;

import com.curso.instrumento2026.api.dto.InstrumentoRequest;
import com.curso.instrumento2026.api.dto.InstrumentoResponse;
import com.curso.instrumento2026.api.mapper.InstrumentoMapper;
import com.curso.instrumento2026.domain.Instrumento;
import com.curso.instrumento2026.service.InstrumentoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import com.curso.instrumento2026.api.dto.InstrumentoAtualizacaoRequest;
import com.curso.instrumento2026.api.dto.StatusRequest;
import com.curso.instrumento2026.api.dto.MovimentacaoEstoqueRequest;
import com.curso.instrumento2026.api.dto.PaginaResponse;
import com.curso.instrumento2026.domain.Status;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/instrumentos")
public class InstrumentoController {

    private final InstrumentoService service;
    private final InstrumentoMapper mapper;

    public InstrumentoController(
            InstrumentoService service,
            InstrumentoMapper mapper) {

        this.service = service;
        this.mapper = mapper;
    }

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

    @GetMapping("/{id}")
    public InstrumentoResponse buscarPorId(
            @PathVariable Long id) {

        return mapper.toResponse(
                service.buscarPorId(id)
        );
    }

    @GetMapping
    public PaginaResponse<InstrumentoResponse> pesquisar(
            @RequestParam(required = false) String nomeInstrumento,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long fornecedorId,
            @RequestParam(required = false) Boolean abaixoEstoqueMinimo,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return PaginaResponse.de(service.pesquisar(nomeInstrumento, status, categoriaId,
                fornecedorId, abaixoEstoqueMinimo, pageable), mapper::toResponse);
    }

    @PutMapping("/{id}")
    public InstrumentoResponse alterar(@PathVariable Long id,
            @Valid @RequestBody InstrumentoAtualizacaoRequest request) {
        return mapper.toResponse(service.alterar(id, request.nomeInstrumento(), request.precoUnitario(),
                request.estoqueMinimo(), request.categoriaId(), request.fornecedorId()));
    }

    @PutMapping("/{id}/status")
    public InstrumentoResponse alterarStatus(@PathVariable Long id,
            @Valid @RequestBody StatusRequest request) {
        return mapper.toResponse(service.alterarStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/estoque/entradas")
    public InstrumentoResponse receberEstoque(@PathVariable Long id,
            @Valid @RequestBody MovimentacaoEstoqueRequest request) {
        return mapper.toResponse(service.receberEstoque(id, request.quantidade()));
    }

    @PostMapping("/{id}/estoque/saidas")
    public InstrumentoResponse retirarEstoque(@PathVariable Long id,
            @Valid @RequestBody MovimentacaoEstoqueRequest request) {
        return mapper.toResponse(service.retirarEstoque(id, request.quantidade()));
    }
}
