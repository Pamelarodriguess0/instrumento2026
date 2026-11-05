package com.curso.instrumento2026.service;

import com.curso.instrumento2026.domain.*;
import com.curso.instrumento2026.exception.RecursoEmUsoException;
import com.curso.instrumento2026.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.SimpleTransactionStatus;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.curso.instrumento2026.support.AdminFixtureSupport.comoAdministrador;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Calls real services through Spring method-security and transaction proxies, without HTTP or database access.
@SpringJUnitConfig(AutorizacaoServicesTest.Configuracao.class)
class AutorizacaoServicesTest {
    enum Operacao { CADASTRAR_CONTA, ALTERAR_PERFIS_CONTA, ALTERAR_STATUS_CONTA, EXCLUIR_CATEGORIA, EXCLUIR_INSTRUMENTO, REMOVER_ITEM }
    private static final String LOGIN = "operador.services";
    private static final String ATUAL = "Senha ficticia atual 2026";
    private static final String NOVA = "Senha ficticia nova 2026";
    private static final String HASH_ATUAL = "hash-ficticio-atual";
    private static final String HASH_NOVO = "hash-ficticio-novo";
    @Autowired ContaAcessoService contas;
    @Autowired CategoriaInstrumentoService categorias;
    @Autowired InstrumentoService instrumentos;
    @Autowired VendaService vendas;
    @Autowired ApplicationContext contexto;
    @MockitoBean ContaAcessoRepository contaRepository;
    @MockitoBean PessoaRepository pessoaRepository;
    @MockitoBean CategoriaInstrumentoRepository categoriaRepository;
    @MockitoBean InstrumentoRepository instrumentoRepository;
    @MockitoBean FornecedorRepository fornecedorRepository;
    @MockitoBean VendaRepository vendaRepository;
    @MockitoBean ClienteRepository clienteRepository;
    @MockitoBean ColaboradorRepository colaboradorRepository;
    @MockitoBean EntityManager entityManager;
    @MockitoBean PasswordEncoder encoder;
    @MockitoBean PlatformTransactionManager transactionManager;
    private ContaAcesso conta;
    private CategoriaInstrumento categoria;
    private Instrumento instrumento;
    private Venda venda;
    private Session session;

    @BeforeEach
    void prepararSomenteObjetosEmMemoria() {
        assertTrue(contexto.getBeansOfType(DataSource.class).isEmpty());
        for (Object service : new Object[]{contas, categorias, instrumentos, vendas}) {
            assertTrue(AopUtils.isAopProxy(service), "A chamada deve passar pelo proxy Spring");
        }
        Pessoa pessoa = new Pessoa("Pessoa ficticia", "services@example.com", "52998224725", LocalDate.now());
        conta = new ContaAcesso(pessoa, LOGIN, HASH_ATUAL);
        categoria = new CategoriaInstrumento(1L, "Cordas ficticias");
        instrumento = new Instrumento(1L, "AUTH-SERVICE", "Instrumento ficticio", 5,
                BigDecimal.TEN, LocalDate.now(), Status.ATIVO, categoria);
        venda = mock(Venda.class);
        session = mock(Session.class);
        when(pessoaRepository.findById(2L)).thenReturn(Optional.of(pessoa));
        when(contaRepository.findOneById(1L)).thenReturn(Optional.of(conta));
        when(contaRepository.findByLoginIgnoreCase(LOGIN)).thenReturn(Optional.of(conta));
        when(contaRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(instrumentoRepository.findById(1L)).thenReturn(Optional.of(instrumento));
        when(vendaRepository.findOneById(1L)).thenReturn(Optional.of(venda));
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(encoder.encode(NOVA)).thenReturn(HASH_NOVO);
        when(encoder.matches(ATUAL, HASH_ATUAL)).thenReturn(true);
        when(transactionManager.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
    }

    @ParameterizedTest
    @EnumSource(Operacao.class)
    @WithMockUser(roles = "OPERADOR")
    void operadorDeveSerNegadoAntesDeConsultarOuEscrever(Operacao operacao) {
        assertThrows(AccessDeniedException.class, () -> executar(operacao));
        semOperacoesExecutadas();
    }

    @ParameterizedTest
    @EnumSource(Operacao.class)
    @WithAnonymousUser
    void anonimoNaoDeveExecutarOperacaoAdministrativa(Operacao operacao) {
        assertThrows(AccessDeniedException.class, () -> executar(operacao));
        semOperacoesExecutadas();
    }

    @ParameterizedTest
    @EnumSource(Operacao.class)
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorDeveExecutarOperacaoEConcluirTransacao(Operacao operacao) {
        assertDoesNotThrow(() -> executar(operacao));
        switch (operacao) {
            case CADASTRAR_CONTA -> {
                verify(contaRepository).saveAndFlush(any(ContaAcesso.class));
                verify(encoder).encode(NOVA);
            }
            case ALTERAR_STATUS_CONTA -> {
                verify(contaRepository).findOneById(1L);
                assertEquals(StatusContaAcesso.BLOQUEADA, conta.getStatus());
            }
            case ALTERAR_PERFIS_CONTA -> assertEquals(
                    java.util.Set.of(PerfilAcesso.OPERADOR, PerfilAcesso.ADMINISTRADOR), conta.getPerfis());
            case EXCLUIR_CATEGORIA -> {
                verify(categoriaRepository).delete(categoria);
                verify(categoriaRepository).flush();
            }
            case EXCLUIR_INSTRUMENTO -> {
                verify(instrumentoRepository).delete(instrumento);
                verify(instrumentoRepository).flush();
            }
            case REMOVER_ITEM -> verify(venda).removerItem(2L);
        }
        verify(transactionManager).commit(any());
        verify(transactionManager, never()).rollback(any());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void negativaDeSegurancaDevePrecederValidacoesDeNegocio() {
        assertThrows(AccessDeniedException.class, () -> contas.cadastrar(null, null, null));
        assertThrows(AccessDeniedException.class, () -> contas.alterarStatus(null, null));
        semOperacoesExecutadas();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorContinuaSujeitoAValidacaoDeConta() {
        var erro = assertThrows(IllegalArgumentException.class, () -> contas.cadastrar(null, LOGIN, NOVA));
        assertEquals("Pessoa é obrigatória", erro.getMessage());
        verifyNoInteractions(contaRepository, pessoaRepository, encoder);
        verify(transactionManager).rollback(any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void categoriaEmUsoDeveContinuarRecusandoExclusaoSemConfundirComSeguranca() {
        when(instrumentoRepository.existsByCategoriaId(1L)).thenReturn(true);
        assertThrows(RecursoEmUsoException.class, () -> categorias.excluir(1L));
        verify(categoriaRepository, never()).delete(any(CategoriaInstrumento.class));
        verify(categoriaRepository, never()).flush();
        verify(transactionManager).rollback(any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void vendaEncerradaDeveContinuarRecusandoRemocaoPorRegraDeNegocio() {
        doThrow(new IllegalStateException("Somente vendas abertas podem ser alteradas")).when(venda).removerItem(2L);
        assertThrows(IllegalStateException.class, () -> vendas.removerItem(1L, 2L));
        verify(venda).removerItem(2L);
        verify(transactionManager).rollback(any());
    }

    @Test
    @WithMockUser(username = LOGIN, roles = "OPERADOR")
    void consultaDaContaETrocaDaPropriaSenhaDevemPermanecerDisponiveis() {
        assertSame(conta, contas.buscarPorLogin(LOGIN));
        assertSame(conta, contas.buscarPorId(1L));
        contas.alterarSenha(LOGIN, ATUAL, NOVA);
        assertEquals(HASH_NOVO, conta.getSenhaHash());
        assertEquals(StatusContaAcesso.ATIVA, conta.getStatus());
        verify(encoder).matches(ATUAL, HASH_ATUAL);
        verify(encoder).encode(NOVA);
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void cadastroEAlteracaoDeCategoriaDevemPermanecerComerciais() {
        var novaCategoria = new CategoriaInstrumento(null, "Categoria ficticia nova");
        assertSame(novaCategoria, categorias.cadastrar(novaCategoria));
        assertSame(categoria, categorias.alterar(1L, "Cordas atualizadas"));
        assertEquals("Cordas atualizadas", categoria.getNome());
        verify(categoriaRepository).save(novaCategoria);
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void estoqueDeveContinuarDisponivelComBloqueioPessimista() {
        instrumentos.receberEstoque(1L, BigDecimal.valueOf(2));
        instrumentos.retirarEstoque(1L, BigDecimal.ONE);
        assertEquals(6, instrumento.getQuantidadeEstoque());
        verify(entityManager, times(2)).lock(instrumento, LockModeType.PESSIMISTIC_WRITE);
        verify(entityManager, times(2)).refresh(instrumento, LockModeType.PESSIMISTIC_WRITE);
        verify(entityManager, never()).flush();
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void consultaECancelamentoDeVendaDevemPermanecerComerciais() {
        assertSame(venda, vendas.buscarPorId(1L));
        assertSame(venda, vendas.cancelar(1L));
        verify(venda).cancelar();
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void fixtureAdministrativaDeveRestaurarContextoDoOperadorAposSucesso() {
        var anterior = SecurityContextHolder.getContext();
        var cadastrada = comoAdministrador(() -> contas.cadastrar(2L, "fixture.conta", NOVA));
        assertEquals("fixture.conta", cadastrada.getLogin());
        assertSame(anterior, SecurityContextHolder.getContext());
        assertThrows(AccessDeniedException.class, () -> instrumentos.excluir(1L));
        verifyNoInteractions(instrumentoRepository);
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void fixtureAdministrativaDeveRestaurarContextoMesmoQuandoFalha() {
        var anterior = SecurityContextHolder.getContext();
        assertThrows(IllegalArgumentException.class, () -> comoAdministrador(() -> contas.cadastrar(null, LOGIN, NOVA)));
        assertSame(anterior, SecurityContextHolder.getContext());
        assertThrows(AccessDeniedException.class, () -> contas.cadastrar(2L, LOGIN, NOVA));
        verifyNoInteractions(contaRepository, pessoaRepository, encoder);
    }

    @Test
    @WithMockUser(username = "admin.tarefa", roles = "ADMINISTRADOR")
    void executorDecoradoDevePropagarContextoParaTarefaERestaurarThread() throws Exception {
        var original = Executors.newFixedThreadPool(1);
        var decorado = new DelegatingSecurityContextExecutorService(original);
        var anterior = SecurityContextHolder.getContext();
        try {
            String nome = decorado.submit(() -> {
                contas.alterarStatus(1L, StatusContaAcesso.BLOQUEADA);
                return SecurityContextHolder.getContext().getAuthentication().getName();
            }).get(5, TimeUnit.SECONDS);
            assertEquals("admin.tarefa", nome);
            assertEquals(StatusContaAcesso.BLOQUEADA, conta.getStatus());
            assertNull(original.submit(() -> SecurityContextHolder.getContext().getAuthentication()).get(5, TimeUnit.SECONDS));
            assertSame(anterior, SecurityContextHolder.getContext());
        } finally {
            decorado.shutdownNow();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @WithMockUser(username = LOGIN, roles = "ADMINISTRADOR")
    void autoprotecaoDeveReverterTransacaoSemAlterarEstado(boolean perfis) {
        conta.concederPerfil(PerfilAcesso.ADMINISTRADOR);
        assertThrows(IllegalArgumentException.class, () -> {
            if (perfis) contas.alterarPerfis(1L, java.util.Set.of(PerfilAcesso.OPERADOR));
            else contas.alterarStatus(1L, StatusContaAcesso.BLOQUEADA);
        });
        assertTrue(conta.possuiPerfil(PerfilAcesso.ADMINISTRADOR));
        assertEquals(StatusContaAcesso.ATIVA, conta.getStatus());
        verify(contaRepository, never()).contarAdministradoresAtivos();
        verify(transactionManager).rollback(any());
        verify(transactionManager, never()).commit(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @WithMockUser(username = "outro.admin", roles = "ADMINISTRADOR")
    void ultimoAdministradorDeveSerPreservadoComRollback(boolean perfis) {
        conta.concederPerfil(PerfilAcesso.ADMINISTRADOR);
        when(contaRepository.contarAdministradoresAtivos()).thenReturn(1L);
        assertThrows(IllegalArgumentException.class, () -> {
            if (perfis) contas.alterarPerfis(1L, java.util.Set.of(PerfilAcesso.OPERADOR));
            else contas.alterarStatus(1L, StatusContaAcesso.BLOQUEADA);
        });
        assertTrue(conta.possuiPerfil(PerfilAcesso.ADMINISTRADOR));
        assertEquals(StatusContaAcesso.ATIVA, conta.getStatus());
        verify(contaRepository).contarAdministradoresAtivos();
        verify(transactionManager).rollback(any());
        verify(transactionManager, never()).commit(any());
    }

    private void executar(Operacao operacao) {
        switch (operacao) {
            case CADASTRAR_CONTA -> contas.cadastrar(2L, "nova.conta", NOVA);
            case ALTERAR_PERFIS_CONTA -> contas.alterarPerfis(1L,
                    java.util.Set.of(PerfilAcesso.OPERADOR, PerfilAcesso.ADMINISTRADOR));
            case ALTERAR_STATUS_CONTA -> contas.alterarStatus(1L, StatusContaAcesso.BLOQUEADA);
            case EXCLUIR_CATEGORIA -> categorias.excluir(1L);
            case EXCLUIR_INSTRUMENTO -> instrumentos.excluir(1L);
            case REMOVER_ITEM -> vendas.removerItem(1L, 2L);
        }
    }

    private void semOperacoesExecutadas() {
        verifyNoInteractions(contaRepository, pessoaRepository, categoriaRepository, instrumentoRepository,
                fornecedorRepository, vendaRepository, clienteRepository, colaboradorRepository,
                entityManager, encoder, transactionManager, venda, session);
        assertEquals(StatusContaAcesso.ATIVA, conta.getStatus());
        assertEquals(HASH_ATUAL, conta.getSenhaHash());
        assertEquals(5, instrumento.getQuantidadeEstoque());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    @EnableTransactionManagement(proxyTargetClass = true)
    @Import({ContaAcessoService.class, CategoriaInstrumentoService.class, InstrumentoService.class, VendaService.class})
    static class Configuracao { }
}
