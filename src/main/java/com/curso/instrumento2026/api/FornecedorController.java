package com.curso.instrumento2026.api;

import com.curso.instrumento2026.api.dto.FornecedorRequest;
import com.curso.instrumento2026.api.dto.FornecedorResponse;
import com.curso.instrumento2026.api.mapper.FornecedorMapper;
import com.curso.instrumento2026.domain.Fornecedor;
import com.curso.instrumento2026.service.FornecedorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/fornecedores")
public class FornecedorController {

    private final FornecedorService service;
    private final FornecedorMapper mapper;

    public FornecedorController(
            FornecedorService service,
            FornecedorMapper mapper) {

        this.service = service;
        this.mapper = mapper;
    }

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

    @GetMapping("/{id}")
    public FornecedorResponse buscarPorId(
            @PathVariable Long id) {

        return mapper.toResponse(
                service.buscarPorId(id)
        );
    }

    @GetMapping
    public List<FornecedorResponse> listar() {

        return service.listar()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}
