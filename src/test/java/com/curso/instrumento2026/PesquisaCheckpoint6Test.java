package com.curso.instrumento2026;

import com.curso.instrumento2026.domain.*;
import com.curso.instrumento2026.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PesquisaCheckpoint6Test {
    @Autowired CategoriaInstrumentoService categorias;
    @Autowired InstrumentoService instrumentos;
    @Autowired EntityManager em;
    private final PageRequest pagina = PageRequest.of(0, 100);

    @Test
    void deveCombinarNomeEStatusDeCategoria() {
        var ativa = categorias.cadastrar(new CategoriaInstrumento(null, "CP6 Cordas ativa"));
        var inativa = categorias.cadastrar(new CategoriaInstrumento(null, "CP6 Cordas inativa"));
        inativa.inativar();
        categorias.cadastrar(new CategoriaInstrumento(null, "CP6 Sopro"));
        assertEquals(2, categorias.pesquisar("  cp6 CORDAS  ", null, pagina).getTotalElements());
        assertEquals(java.util.List.of(ativa), categorias.pesquisar("cp6 cordas", Status.ATIVO, pagina).getContent());
        assertEquals(java.util.List.of(inativa), categorias.pesquisar("cp6 cordas", Status.INATIVO, pagina).getContent());
        assertEquals(0, categorias.pesquisar("cp6 ausente", null, pagina).getTotalElements());
        assertEquals(categorias.listar().size(), categorias.pesquisar("  ", null, pagina).getTotalElements());
    }

    @Test
    void deveAplicarCadaFiltroECombinarTodosNoBanco() {
        var categoria = categorias.cadastrar(new CategoriaInstrumento(null, "CP6 instrumentos"));
        var outra = categorias.cadastrar(new CategoriaInstrumento(null, "CP6 outra"));
        var fornecedor = new Fornecedor("Fornecedor CP6", "23456789000188");
        em.persist(fornecedor);
        var alvo = criar("CP6-A", "CP6 Violino", categoria, fornecedor, 2, "2.5");
        var igual = criar("CP6-B", "CP6 Violao", categoria, null, 3, "3");
        var inativo = criar("CP6-C", "CP6 Flauta", outra, fornecedor, 4, "1");
        inativo.inativar();
        em.flush();
        em.clear();
        assertEquals(3, pesquisar(null, null, null, null, null));
        assertEquals(2, pesquisar(null, Status.ATIVO, null, null, null));
        assertEquals(2, pesquisar(null, null, categoria.getId(), null, null));
        assertEquals(2, pesquisar(null, null, null, fornecedor.getId(), null));
        assertEquals(1, pesquisar(null, null, null, null, true));
        assertEquals(3, pesquisar(null, null, null, null, false));
        assertEquals(3, pesquisar("  ", null, null, null, null));
        var resultado = instrumentos.pesquisar("  CP6 VIOL  ", Status.ATIVO,
                categoria.getId(), fornecedor.getId(), true, pagina);
        assertEquals(1, resultado.getTotalElements());
        assertEquals(alvo.getId(), resultado.getContent().getFirst().getId());
        assertEquals(0, pesquisar(null, null, Long.MAX_VALUE, null, null));
        assertEquals(0, pesquisar(null, null, null, Long.MAX_VALUE, null));
        assertNotNull(igual.getId());
    }

    private long pesquisar(String nome, Status status, Long categoria, Long fornecedor, Boolean abaixo) {
        return instrumentos.pesquisar(nome == null ? "CP6" : nome, status, categoria, fornecedor, abaixo, pagina).getTotalElements();
    }

    private Instrumento criar(String codigo, String nome, CategoriaInstrumento categoria,
                             Fornecedor fornecedor, int saldo, String minimo) {
        return instrumentos.cadastrar(new Instrumento(codigo, nome, saldo, BigDecimal.TEN,
                new BigDecimal(minimo), LocalDate.now()), categoria.getId(), fornecedor == null ? null : fornecedor.getId());
    }
}
