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
import java.util.List;

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
    public List<InstrumentoResponse> listar() {

        return service.listar()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}
