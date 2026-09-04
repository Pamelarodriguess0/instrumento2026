package com.curso.instrumento2026;

import com.curso.instrumento2026.domain.CategoriaInstrumento;
import com.curso.instrumento2026.domain.Instrumento;
import com.curso.instrumento2026.domain.Status;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PersistenciaJpaTest {

    @PersistenceContext
    private EntityManager entityManager;
    @Test
    void devePersistirERelerInstrumentoComCategoria() {
        CategoriaInstrumento categoria =
                new CategoriaInstrumento(null, "Cordas");

        Instrumento instrumento = new Instrumento(
                null,
                "INT001",
                "Violoncelo Eagle",
                5,
                new BigDecimal("5000.00"),
                LocalDate.of(2026, 6, 15),
                Status.ATIVO,
                categoria
        );

        categoria.adicionarInstrumento(instrumento);
        entityManager.persist(categoria);
        entityManager.persist(instrumento);
        entityManager.flush();
        Long instrumentoId = instrumento.getId();
        entityManager.clear();
        Instrumento instrumentoSalvo =
                entityManager.find(Instrumento.class, instrumentoId);

        assertNotNull(instrumentoSalvo);
        assertEquals("INT001", instrumentoSalvo.getCodigoInstrumento());
        assertEquals("Violoncelo Eagle", instrumentoSalvo.getNomeInstrumento());
        assertEquals(5, instrumentoSalvo.getQuantidadeEstoque());
        assertEquals("Cordas", instrumentoSalvo.getCategoria().getNome());
    }

    @Test
    void naoDevePermitirEstoqueNegativoNoBanco() {
        CategoriaInstrumento categoria =
                new CategoriaInstrumento(null, "Madeiras");

        entityManager.persist(categoria);
        entityManager.flush();
        assertThrows(PersistenceException.class, () -> {

            entityManager.createNativeQuery("""
                INSERT INTO instrumento (
                    codigo_instrumento,
                    nome_instrumento,
                    quantidade_estoque,
                    estoque_minimo,
                    preco_unitario,
                    data_cadastro,
                    status,
                    categoria_instrumento_id
                )
                VALUES (
                    'INT200',
                    'Clarinete Teste',
                    -5,
                    0,
                    2500.00,
                    '2026-08-26',
                    'ATIVO',
                    :categoriaId
                )
                """)
                    .setParameter("categoriaId", categoria.getId())
                    .executeUpdate();

            entityManager.flush();
        });
    }
}
