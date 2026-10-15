package com.curso.instrumento2026;

import com.curso.instrumento2026.domain.CategoriaInstrumento;
import com.curso.instrumento2026.exception.RecursoDuplicadoException;
import com.curso.instrumento2026.repository.CategoriaInstrumentoRepository;
import com.curso.instrumento2026.service.CategoriaInstrumentoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NormalizacaoCategoriaTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CategoriaInstrumentoService service;
    @Autowired private CategoriaInstrumentoRepository repository;

    @Test
    void deveTerOs18ChangeSetsDaAulaAplicados() {
        assertEquals(18, jdbc.queryForObject("SELECT COUNT(*) FROM databasechangelog WHERE "
                + "filename IN ('db/changelog/changes/001-create-categoria-instrumento.yaml', "
                + "'db/changelog/changes/002-create-instrumento.yaml', "
                + "'db/changelog/changes/003-fornecedor-e-estoque-minimo.yaml', "
                + "'db/changelog/changes/004-unique-nome-categoria-instrumento.yaml')", Integer.class));
    }

    private Long inserirLegado(String nome) {
        return jdbc.queryForObject("INSERT INTO categoria_instrumento (nome, status) "
                + "VALUES (?, 'ATIVO') RETURNING id", Long.class, nome);
    }

    @Test
    void bancoDeveRejeitarNomeDuplicadoPorCaixa() {
        inserirLegado("CP8 unicidade caixa");
        assertThrows(DataIntegrityViolationException.class,
                () -> inserirLegado("CP8 UNICIDADE CAIXA"));
    }

    @Test
    void bancoDeveRejeitarNomeDuplicadoPorCaixaEEspacos() {
        inserirLegado("  CP8 unicidade espacos  ");
        assertThrows(DataIntegrityViolationException.class,
                () -> inserirLegado("CP8 UNICIDADE ESPACOS"));
    }

    @Test
    void deveConsultarNomeLegadoNormalizadoSemConsiderarProprioId() {
        Long id = inserirLegado("  CP8 consulta legado  ");
        assertTrue(repository.existsByNomeIgnoreCase("CP8 CONSULTA LEGADO"));
        assertEquals(id, repository.findByNomeIgnoreCase(" CP8 CONSULTA LEGADO ").orElseThrow().getId());
        assertFalse(repository.existsByNomeIgnoreCaseAndIdNot("CP8 consulta legado", id));
        service.alterar(id, "  CP8 CONSULTA LEGADO  ");
        repository.flush();
        assertEquals("CP8 CONSULTA LEGADO", jdbc.queryForObject(
                "SELECT nome FROM categoria_instrumento WHERE id = ?", String.class, id));
    }

    @Test
    void cadastroDeveDetectarDuplicidadeComNomeLegado() {
        inserirLegado("  CP8 cadastro legado  ");
        assertThrows(RecursoDuplicadoException.class,
                () -> service.cadastrar(new CategoriaInstrumento(null, "CP8 CADASTRO LEGADO")));
    }

    @Test
    void alteracaoDeveDetectarDuplicidadeComNomeLegadoSemAlterarOutroRegistro() {
        inserirLegado("  CP8 alteracao legado  ");
        var outra = service.cadastrar(new CategoriaInstrumento(null, "CP8 outra"));
        assertThrows(RecursoDuplicadoException.class,
                () -> service.alterar(outra.getId(), " CP8 ALTERACAO LEGADO "));
        assertEquals("CP8 outra", outra.getNome());
    }

    @Test
    void cadastroDeveGravarNomeSemEspacosExternos() {
        var categoria = service.cadastrar(new CategoriaInstrumento(null, "  CP8 cadastro normalizado  "));
        repository.flush();
        assertEquals("CP8 cadastro normalizado", jdbc.queryForObject(
                "SELECT nome FROM categoria_instrumento WHERE id = ?", String.class, categoria.getId()));
    }
}
