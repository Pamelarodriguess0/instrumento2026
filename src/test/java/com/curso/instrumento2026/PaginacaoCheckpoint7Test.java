package com.curso.instrumento2026;

import com.curso.instrumento2026.api.dto.PaginaResponse;
import com.curso.instrumento2026.domain.CategoriaInstrumento;
import com.curso.instrumento2026.domain.Instrumento;
import com.curso.instrumento2026.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaginacaoCheckpoint7Test {
    @Autowired CategoriaInstrumentoService categorias;
    @Autowired InstrumentoService instrumentos;
    @Autowired RequestMappingHandlerAdapter adapter;

    @Test
    void devePaginarEOrdenarCategoriasComMetadados() {
        for (String nome : List.of("CP7 C", "CP7 A", "CP7 B")) {
            categorias.cadastrar(new CategoriaInstrumento(null, nome));
        }
        var primeira = PaginaResponse.de(categorias.pesquisar("CP7", null,
                PageRequest.of(0, 2, Sort.by("nome"))), CategoriaInstrumento::getNome);
        assertEquals(List.of("CP7 A", "CP7 B"), primeira.conteudo());
        assertEquals(0, primeira.pagina());
        assertEquals(2, primeira.tamanho());
        assertEquals(3, primeira.totalElementos());
        assertEquals(2, primeira.totalPaginas());
        assertTrue(primeira.primeira());
        assertFalse(primeira.ultima());
        var ultima = PaginaResponse.de(categorias.pesquisar("CP7", null,
                PageRequest.of(1, 2, Sort.by("nome"))), CategoriaInstrumento::getNome);
        assertEquals(List.of("CP7 C"), ultima.conteudo());
        assertEquals(1, ultima.pagina());
        assertFalse(ultima.primeira());
        assertTrue(ultima.ultima());
        assertTrue(categorias.pesquisar("CP7", null, PageRequest.of(2, 2)).isEmpty());
    }

    @Test
    void devePaginarInstrumentosComOrdenacaoMultipla() {
        var categoria = categorias.cadastrar(new CategoriaInstrumento(null, "CP7 instrumentos"));
        for (int i = 1; i <= 3; i++) {
            instrumentos.cadastrar(new Instrumento("CP7-" + i, "CP7 Violino", i,
                    BigDecimal.TEN, BigDecimal.ZERO, LocalDate.now()), categoria.getId(), null);
        }
        var sort = Sort.by("nomeInstrumento").ascending().and(Sort.by("quantidadeEstoque").descending());
        var pagina = instrumentos.pesquisar("CP7", null, categoria.getId(), null, null, PageRequest.of(1, 1, sort));
        var resposta = PaginaResponse.de(pagina, Instrumento::getCodigoInstrumento);
        assertEquals(List.of("CP7-2"), resposta.conteudo());
        assertEquals(3, resposta.totalElementos());
        assertEquals(3, resposta.totalPaginas());
        assertEquals(1, resposta.tamanho());
        assertFalse(resposta.primeira());
        assertFalse(resposta.ultima());
    }

    @Test
    void deveAceitarSomenteCamposDeOrdenacaoPermitidos() {
        for (String campo : List.of("id", "nome", "status")) {
            assertDoesNotThrow(() -> categorias.pesquisar(null, null, PageRequest.of(0, 2, Sort.by(campo))));
        }
        for (String campo : List.of("id", "codigoInstrumento", "nomeInstrumento", "quantidadeEstoque",
                "precoUnitario", "estoqueMinimo", "dataCadastro", "status")) {
            assertDoesNotThrow(() -> instrumentos.pesquisar(null, null, null, null, null, PageRequest.of(0, 2, Sort.by(campo))));
        }
        for (String campo : List.of("inexistente", "categoria.nome", "fornecedor.razaoSocial")) {
            var pageable = PageRequest.of(0, 2, Sort.by("id").and(Sort.by(campo)));
            assertEquals("Campo de ordenação inválido: " + campo, assertThrows(IllegalArgumentException.class,
                    () -> categorias.pesquisar(null, null, pageable)).getMessage());
            assertEquals("Campo de ordenação inválido: " + campo, assertThrows(IllegalArgumentException.class,
                    () -> instrumentos.pesquisar(null, null, null, null, null, pageable)).getMessage());
        }
    }

    @Test
    void deveRepresentarPaginaVazia() {
        var resposta = PaginaResponse.de(categorias.pesquisar("CP7 inexistente", null, PageRequest.of(0, 20)), CategoriaInstrumento::getNome);
        assertTrue(resposta.conteudo().isEmpty());
        assertEquals(0, resposta.totalElementos());
        assertEquals(0, resposta.totalPaginas());
        assertEquals(20, resposta.tamanho());
        assertTrue(resposta.primeira());
        assertTrue(resposta.ultima());
    }

    @Test
    void deveAplicarPadrao20ELimite100NoResolverRealSemCriarRotas() throws Exception {
        var resolver = adapter.getArgumentResolvers().stream()
                .filter(PageableHandlerMethodArgumentResolver.class::isInstance)
                .map(PageableHandlerMethodArgumentResolver.class::cast).findFirst().orElseThrow();
        var parametro = new MethodParameter(getClass().getDeclaredMethod("parametroDeTeste", Pageable.class), 0);
        var request = new MockHttpServletRequest();
        assertEquals(20, resolver.resolveArgument(parametro, null, new ServletWebRequest(request), null).getPageSize());
        request.setParameter("size", "200");
        assertEquals(100, resolver.resolveArgument(parametro, null, new ServletWebRequest(request), null).getPageSize());
        request.setParameter("size", "5");
        request.setParameter("page", "2");
        var pagina = resolver.resolveArgument(parametro, null, new ServletWebRequest(request), null);
        assertEquals(5, pagina.getPageSize());
        assertEquals(2, pagina.getPageNumber());
    }

    private void parametroDeTeste(Pageable pageable) { }
}
