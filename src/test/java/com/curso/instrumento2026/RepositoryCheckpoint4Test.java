package com.curso.instrumento2026;

import com.curso.instrumento2026.api.mapper.InstrumentoMapper;
import com.curso.instrumento2026.domain.CategoriaInstrumento;
import com.curso.instrumento2026.domain.Fornecedor;
import com.curso.instrumento2026.domain.Instrumento;
import com.curso.instrumento2026.repository.CategoriaInstrumentoRepository;
import com.curso.instrumento2026.repository.InstrumentoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RepositoryCheckpoint4Test {

    @Autowired
    private CategoriaInstrumentoRepository categoriaRepository;
    @Autowired
    private InstrumentoRepository instrumentoRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private InstrumentoMapper mapper;

    @Test
    void deveVerificarNomeIgnorandoCaixaEExcluindoOProprioId() {
        CategoriaInstrumento categoria = categoriaRepository.save(
                new CategoriaInstrumento(null, "Categoria CP4 nome"));
        CategoriaInstrumento outra = categoriaRepository.save(
                new CategoriaInstrumento(null, "Categoria CP4 outra"));

        assertFalse(categoriaRepository.existsByNomeIgnoreCaseAndIdNot(
                "CATEGORIA CP4 NOME", categoria.getId()));
        assertTrue(categoriaRepository.existsByNomeIgnoreCaseAndIdNot(
                "CATEGORIA CP4 NOME", outra.getId()));
        assertFalse(categoriaRepository.existsByNomeIgnoreCaseAndIdNot(
                "Categoria CP4 inexistente", outra.getId()));
    }

    @Test
    void deveIdentificarCategoriaComESemInstrumentos() {
        CategoriaInstrumento categoria = categoriaRepository.save(
                new CategoriaInstrumento(null, "Categoria CP4 utilizada"));
        CategoriaInstrumento vazia = categoriaRepository.save(
                new CategoriaInstrumento(null, "Categoria CP4 vazia"));
        persistirInstrumento("CP4-USO", categoria, null);

        assertTrue(instrumentoRepository.existsByCategoriaId(categoria.getId()));
        assertFalse(instrumentoRepository.existsByCategoriaId(vazia.getId()));
        assertFalse(instrumentoRepository.existsByCategoriaId(Long.MAX_VALUE));
    }

    @Test
    void deveCarregarRelacionamentosNaPaginaInclusiveFornecedorOpcional() {
        CategoriaInstrumento categoria = categoriaRepository.save(
                new CategoriaInstrumento(null, "Categoria CP4 pagina"));
        Fornecedor fornecedor = new Fornecedor("Fornecedor CP4", "87654321000188");
        entityManager.persist(fornecedor);
        persistirInstrumento("CP4-COM-FORNECEDOR", categoria, fornecedor);
        persistirInstrumento("CP4-SEM-FORNECEDOR", categoria, null);
        entityManager.flush();
        entityManager.clear();

        Specification<Instrumento> filtro = (root, query, cb) ->
                cb.equal(root.get("categoria").get("id"), categoria.getId());
        var pagina = instrumentoRepository.findAll(filtro, PageRequest.of(0, 10));
        var util = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

        assertEquals(2, pagina.getTotalElements());
        for (Instrumento instrumento : pagina.getContent()) {
            assertTrue(util.isLoaded(instrumento, "categoria"));
            assertTrue(util.isLoaded(instrumento, "fornecedor"));
        }
        entityManager.clear();

        var respostas = pagina.getContent().stream().map(mapper::toResponse).toList();
        assertTrue(respostas.stream().allMatch(r ->
                "Categoria CP4 pagina".equals(r.categoriaNome())));
        assertEquals(1, respostas.stream().filter(r ->
                "Fornecedor CP4".equals(r.fornecedorRazaoSocial())).count());
        assertEquals(1, respostas.stream().filter(r -> r.fornecedorId() == null).count());
    }

    private void persistirInstrumento(
            String codigo, CategoriaInstrumento categoria, Fornecedor fornecedor) {
        Instrumento instrumento = new Instrumento(
                codigo, "Instrumento CP4", 5, new BigDecimal("100.00"),
                BigDecimal.ZERO, LocalDate.now());
        instrumento.definirCategoria(categoria);
        instrumento.definirFornecedor(fornecedor);
        instrumentoRepository.save(instrumento);
    }
}
