package com.curso.instrumento2026.service;

import com.curso.instrumento2026.config.AdministradorInicialProperties;
import com.curso.instrumento2026.config.PasswordEncoderConfig;
import com.curso.instrumento2026.domain.*;
import com.curso.instrumento2026.exception.RecursoDuplicadoException;
import com.curso.instrumento2026.repository.ContaAcessoRepository;
import com.curso.instrumento2026.repository.PessoaRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.sql.DataSource;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(AdministradorInicialServiceTest.Configuracao.class)
class AdministradorInicialServiceTest {
    private static final String SENHA = "  Frase ficticia inicial 2026  ";
    @Autowired AdministradorInicialService service;
    @Autowired ContaAcessoService protegido;
    @Autowired PasswordEncoder encoder;
    @Autowired ApplicationContext contexto;
    @MockitoBean ContaAcessoRepository contas;
    @MockitoBean PessoaRepository pessoas;
    @MockitoBean PlatformTransactionManager transactionManager;
    private AtomicReference<ContaAcesso> gravada;

    @BeforeEach
    void prepararSemPersistenciaReal() {
        assertTrue(contexto.getBeansOfType(DataSource.class).isEmpty());
        assertTrue(AopUtils.isAopProxy(service));
        assertTrue(AopUtils.isAopProxy(protegido));
        gravada = new AtomicReference<>();
        when(contas.contarAdministradoresAtivos()).thenAnswer(invocation -> gravada.get() != null
                && gravada.get().podeAutenticar() && gravada.get().possuiPerfil(PerfilAcesso.ADMINISTRADOR) ? 1L : 0L);
        when(pessoas.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(contas.saveAndFlush(any())).thenAnswer(invocation -> {
            ContaAcesso conta = invocation.getArgument(0);
            gravada.set(conta);
            return conta;
        });
        when(transactionManager.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
    }

    private static AdministradorInicialProperties propriedades() {
        return new AdministradorInicialProperties(true, "Administrador ficticio", "ADMIN.INICIAL@example.com",
                "52998224725", "  ADMIN.INICIAL  ", SENHA);
    }

    @Test
    void desabilitadoNaoDeveConsultarOuCriarRegistrosMesmoSemConfiguracoes() {
        assertTrue(service.criarSeNecessario(new AdministradorInicialProperties(false, null, null, null, null, null)).isEmpty());
        verifyNoInteractions(contas, pessoas);
    }

    @Test
    void deveCriarPessoaEContaComDoisPerfisSemUsuarioAutenticado() {
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        ContaAcesso conta = service.criarSeNecessario(propriedades()).orElseThrow();
        assertEquals("Administrador ficticio", conta.getPessoa().getNome());
        assertEquals("admin.inicial@example.com", conta.getPessoa().getEmail());
        assertEquals("52998224725", conta.getPessoa().getCpf());
        assertEquals(Status.ATIVO, conta.getPessoa().getStatus());
        assertEquals(StatusContaAcesso.ATIVA, conta.getStatus());
        assertEquals("admin.inicial", conta.getLogin());
        assertEquals(Set.of(PerfilAcesso.OPERADOR, PerfilAcesso.ADMINISTRADOR), conta.getPerfis());
        assertTrue(conta.getSenhaHash().startsWith("{bcrypt}"));
        assertNotEquals(SENHA, conta.getSenhaHash());
        assertTrue(encoder.matches(SENHA, conta.getSenhaHash()));
        assertFalse(encoder.matches(SENHA.trim(), conta.getSenhaHash()));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        var ordem = inOrder(pessoas, contas);
        ordem.verify(pessoas).save(conta.getPessoa());
        ordem.verify(contas).saveAndFlush(conta);
        verify(transactionManager).commit(any());
        verify(transactionManager, never()).rollback(any());
    }

    @Test
    void segundaExecucaoNaoDeveDuplicarPessoaOuConta() {
        var primeira = service.criarSeNecessario(propriedades()).orElseThrow();
        assertTrue(service.criarSeNecessario(propriedades()).isEmpty());
        assertSame(primeira, gravada.get());
        verify(pessoas, times(1)).save(any());
        verify(contas, times(1)).saveAndFlush(any());
    }

    @Test
    void administradorExistenteDispensaCriacaoMesmoSemDemaisPropriedades() {
        when(contas.contarAdministradoresAtivos()).thenReturn(1L);
        assertTrue(service.criarSeNecessario(new AdministradorInicialProperties(true, null, null, null, null, null)).isEmpty());
        verify(contas).contarAdministradoresAtivos();
        verifyNoMoreInteractions(contas);
        verifyNoInteractions(pessoas);
    }

    static Stream<Arguments> invalidas() {
        var p = propriedades();
        return Stream.of(
                Arguments.of("nome ausente", new AdministradorInicialProperties(true, null, p.email(), p.cpf(), p.login(), p.senha())),
                Arguments.of("nome vazio", new AdministradorInicialProperties(true, " ", p.email(), p.cpf(), p.login(), p.senha())),
                Arguments.of("nome longo", new AdministradorInicialProperties(true, "a".repeat(151), p.email(), p.cpf(), p.login(), p.senha())),
                Arguments.of("email ausente", new AdministradorInicialProperties(true, p.nome(), null, p.cpf(), p.login(), p.senha())),
                Arguments.of("email invalido", new AdministradorInicialProperties(true, p.nome(), "invalido", p.cpf(), p.login(), p.senha())),
                Arguments.of("email longo", new AdministradorInicialProperties(true, p.nome(), "a".repeat(180) + "@example.com", p.cpf(), p.login(), p.senha())),
                Arguments.of("cpf ausente", new AdministradorInicialProperties(true, p.nome(), p.email(), null, p.login(), p.senha())),
                Arguments.of("cpf invalido", new AdministradorInicialProperties(true, p.nome(), p.email(), "52998224724", p.login(), p.senha())),
                Arguments.of("login ausente", new AdministradorInicialProperties(true, p.nome(), p.email(), p.cpf(), null, p.senha())),
                Arguments.of("login curto", new AdministradorInicialProperties(true, p.nome(), p.email(), p.cpf(), "ab", p.senha())),
                Arguments.of("login longo", new AdministradorInicialProperties(true, p.nome(), p.email(), p.cpf(), "a".repeat(81), p.senha())),
                Arguments.of("login invalido", new AdministradorInicialProperties(true, p.nome(), p.email(), p.cpf(), "admin local", p.senha())),
                Arguments.of("senha ausente", new AdministradorInicialProperties(true, p.nome(), p.email(), p.cpf(), p.login(), null)),
                Arguments.of("senha curta", new AdministradorInicialProperties(true, p.nome(), p.email(), p.cpf(), p.login(), "a".repeat(11))),
                Arguments.of("senha unicode curta", new AdministradorInicialProperties(true, p.nome(), p.email(), p.cpf(), p.login(), "𝄞".repeat(11))),
                Arguments.of("senha multibyte longa", new AdministradorInicialProperties(true, p.nome(), p.email(), p.cpf(), p.login(), "é".repeat(37))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidas")
    void configuracaoInvalidaDeveFalharAntesDeEscrever(String caso, AdministradorInicialProperties p) {
        var erro = assertThrows(IllegalArgumentException.class, () -> service.criarSeNecessario(p));
        assertFalse(erro.toString().contains(SENHA));
        verify(pessoas, never()).save(any());
        verify(contas, never()).saveAndFlush(any());
        verify(transactionManager).rollback(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"login", "cpf", "email"})
    void duplicidadeNaoDevePromoverOuAlterarRegistroExistente(String campo) {
        switch (campo) {
            case "login" -> when(contas.existsByLoginIgnoreCase("admin.inicial")).thenReturn(true);
            case "cpf" -> when(pessoas.existsByCpf("52998224725")).thenReturn(true);
            case "email" -> when(pessoas.existsByEmailIgnoreCase("admin.inicial@example.com")).thenReturn(true);
            default -> fail(campo);
        }
        assertThrows(RecursoDuplicadoException.class, () -> service.criarSeNecessario(propriedades()));
        verify(pessoas, never()).save(any());
        verify(contas, never()).saveAndFlush(any());
        verify(transactionManager).rollback(any());
    }

    @Test
    void falhaAoSalvarContaDeveSolicitarRollbackDaMesmaTransacaoDaPessoaSemExporCausa() {
        when(contas.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("detalhe SQL sensivel ficticio"));
        var erro = assertThrows(IllegalStateException.class, () -> service.criarSeNecessario(propriedades()));
        assertEquals("Não foi possível provisionar o administrador inicial", erro.getMessage());
        assertNull(erro.getCause());
        verify(pessoas).save(any());
        verify(transactionManager, times(1)).getTransaction(any());
        verify(transactionManager).rollback(any());
        verify(transactionManager, never()).commit(any());
    }

    @Test
    void falhaAoSalvarPessoaNaoDeveTentarSalvarConta() {
        when(pessoas.save(any())).thenThrow(new DataIntegrityViolationException("detalhe interno ficticio"));
        assertThrows(IllegalStateException.class, () -> service.criarSeNecessario(propriedades()));
        verify(contas, never()).saveAndFlush(any());
        verify(transactionManager).rollback(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "é"})
    void senhaNoLimiteUtf8DeveSerAceita(String caractere) {
        var p = propriedades();
        String senha = caractere.repeat(caractere.equals("a") ? 72 : 36);
        var conta = service.criarSeNecessario(new AdministradorInicialProperties(true, p.nome(), p.email(), p.cpf(), p.login(), senha)).orElseThrow();
        assertTrue(encoder.matches(senha, conta.getSenhaHash()));
        assertFalse(encoder.matches(senha + "X", conta.getSenhaHash()));
    }

    @Test
    void senhaComDozePontosDeCodigoUnicodeDeveSerAceita() {
        var p = propriedades();
        String senha = "𝄞".repeat(12);
        var conta = service.criarSeNecessario(new AdministradorInicialProperties(true, p.nome(), p.email(), p.cpf(), p.login(), senha)).orElseThrow();
        assertTrue(encoder.matches(senha, conta.getSenhaHash()));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void provisionamentoNaoDeveEnfraquecerCadastroProtegidoNemElevarIdentidade() {
        var anterior = SecurityContextHolder.getContext();
        assertThrows(AccessDeniedException.class, () -> protegido.cadastrar(2L, "outra.conta", SENHA));
        assertTrue(service.criarSeNecessario(propriedades()).isPresent());
        assertSame(anterior, SecurityContextHolder.getContext());
        assertThrows(AccessDeniedException.class, () -> protegido.cadastrar(2L, "outra.conta", SENHA));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    @EnableTransactionManagement(proxyTargetClass = true)
    @Import({AdministradorInicialService.class, ContaAcessoService.class, PasswordEncoderConfig.class})
    static class Configuracao {
        @Bean
        Validator validator() { return new LocalValidatorFactoryBean(); }
    }
}
