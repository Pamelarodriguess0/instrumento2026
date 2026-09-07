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
import java.util.List;

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
    public List<CategoriaInstrumentoResponse> listar() {

        return service.listar()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}
