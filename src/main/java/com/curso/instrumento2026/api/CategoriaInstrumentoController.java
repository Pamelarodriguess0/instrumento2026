package com.curso.instrumento2026.api;

import com.curso.instrumento2026.api.dto.CategoriaInstrumentoRequest;
import com.curso.instrumento2026.api.dto.CategoriaInstrumentoResponse;
import com.curso.instrumento2026.api.mapper.CategoriaInstrumentoMapper;
import com.curso.instrumento2026.domain.CategoriaInstrumento;
import com.curso.instrumento2026.service.CategoriaInstrumentoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import com.curso.instrumento2026.api.dto.CategoriaInstrumentoAtualizacaoRequest;
import com.curso.instrumento2026.api.dto.StatusRequest;
import com.curso.instrumento2026.api.dto.PaginaResponse;
import com.curso.instrumento2026.domain.Status;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaInstrumentoController {

    private final CategoriaInstrumentoService service;
    private final CategoriaInstrumentoMapper mapper;

    public CategoriaInstrumentoController(
            CategoriaInstrumentoService service,
            CategoriaInstrumentoMapper mapper) {

        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<CategoriaInstrumentoResponse> cadastrar(
            @Valid @RequestBody CategoriaInstrumentoRequest request) {

        CategoriaInstrumento categoria = mapper.toEntity(request);

        CategoriaInstrumento cadastrada =
                service.cadastrar(categoria);

        URI location =
                URI.create("/api/categorias/" + cadastrada.getId());

        return ResponseEntity
                .created(location)
                .body(mapper.toResponse(cadastrada));
    }

    @GetMapping("/{id}")
    public CategoriaInstrumentoResponse buscarPorId(
            @PathVariable Long id) {

        return mapper.toResponse(
                service.buscarPorId(id)
        );
    }

    @GetMapping
    public PaginaResponse<CategoriaInstrumentoResponse> pesquisar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Status status,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return PaginaResponse.de(service.pesquisar(nome, status, pageable), mapper::toResponse);
    }

    @PutMapping("/{id}")
    public CategoriaInstrumentoResponse alterar(@PathVariable Long id,
            @Valid @RequestBody CategoriaInstrumentoAtualizacaoRequest request) {
        return mapper.toResponse(service.alterar(id, request.nome()));
    }

    @PutMapping("/{id}/status")
    public CategoriaInstrumentoResponse alterarStatus(@PathVariable Long id,
            @Valid @RequestBody StatusRequest request) {
        return mapper.toResponse(service.alterarStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
