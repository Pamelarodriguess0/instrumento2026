package com.curso.instrumento2026.config;

import com.curso.instrumento2026.domain.ContaAcesso;
import com.curso.instrumento2026.service.AdministradorInicialService;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.support.ResourcePropertySource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdministradorInicialConfigTest {
    private static final String SENHA = "Senha exclusivamente ficticia 2026";
    private ApplicationContextRunner contexto(AdministradorInicialService service) {
        return new ApplicationContextRunner()
                .withUserConfiguration(AdministradorInicialConfig.class, AdministradorInicialRunner.class)
                .withBean(AdministradorInicialService.class, () -> service);
    }

    @Test
    void runnerDeveEstarAusentePorPadraoSemExigirCredenciais() {
        var service = mock(AdministradorInicialService.class);
        contexto(service).run(ctx -> {
            assertNull(ctx.getStartupFailure());
            assertFalse(ctx.getBean(AdministradorInicialProperties.class).enabled());
            assertEquals(0, ctx.getBeansOfType(AdministradorInicialRunner.class).size());
            verifyNoInteractions(service);
        });
    }

    @Test
    void flagFalseDeveManterRunnerAusente() {
        var service = mock(AdministradorInicialService.class);
        contexto(service).withPropertyValues("app.security.initial-admin.enabled=false").run(ctx -> {
            assertNull(ctx.getStartupFailure());
            assertTrue(ctx.getBeansOfType(AdministradorInicialRunner.class).isEmpty());
            verifyNoInteractions(service);
        });
    }

    @Test
    void propriedadesDaAplicacaoDevemUsarVariaveisExternasEDefaultFalse() throws Exception {
        var service = mock(AdministradorInicialService.class);
        var fonte = new ResourcePropertySource("classpath:application.properties");
        assertEquals("${INITIAL_ADMIN_ENABLED:false}", fonte.getProperty("app.security.initial-admin.enabled"));
        contexto(service).withInitializer(ctx -> ctx.getEnvironment().getPropertySources().addLast(fonte))
                .withPropertyValues("INITIAL_ADMIN_ENABLED=true", "INITIAL_ADMIN_NAME=Administrador ficticio",
                        "INITIAL_ADMIN_EMAIL=admin@example.com", "INITIAL_ADMIN_CPF=52998224725",
                        "INITIAL_ADMIN_LOGIN=admin.inicial", "INITIAL_ADMIN_PASSWORD=" + SENHA)
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());
                    var p = ctx.getBean(AdministradorInicialProperties.class);
                    assertTrue(p.enabled());
                    assertEquals("Administrador ficticio", p.nome());
                    assertEquals("admin@example.com", p.email());
                    assertEquals("52998224725", p.cpf());
                    assertEquals("admin.inicial", p.login());
                    assertEquals(SENHA, p.senha());
                    assertEquals(1, ctx.getBeansOfType(AdministradorInicialRunner.class).size());
                    verifyNoInteractions(service); // ContextRunner does not invoke ApplicationRunner automatically.
                });
    }

    @Test
    void propertiesNaoDevemExporSenhaOuDadosPessoaisNoToString() {
        var p = new AdministradorInicialProperties(true, "Nome ficticio", "admin@example.com", "52998224725", "admin.inicial", SENHA);
        assertFalse(p.toString().contains(SENHA));
        assertFalse(p.toString().contains(p.cpf()));
        assertFalse(p.toString().contains(p.email()));
        assertTrue(p.toString().contains("PROTEGIDOS"));
    }

    @Test
    void runnerDeveRegistrarSomenteMensagensFixasDepoisDoRetornoDoService() {
        var service = mock(AdministradorInicialService.class);
        var p = new AdministradorInicialProperties(true, "Nome ficticio", "admin@example.com", "52998224725", "admin.inicial", SENHA);
        when(service.criarSeNecessario(p)).thenReturn(Optional.of(mock(ContaAcesso.class)), Optional.empty());
        Logger logger = (Logger) LoggerFactory.getLogger(AdministradorInicialRunner.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            var runner = new AdministradorInicialRunner(service, p);
            runner.run(new DefaultApplicationArguments());
            runner.run(new DefaultApplicationArguments());
            assertEquals(2, appender.list.size());
            assertTrue(appender.list.get(0).getFormattedMessage().contains("Administrador inicial criado"));
            assertTrue(appender.list.get(1).getFormattedMessage().contains("dispensado"));
            for (var evento : appender.list) {
                assertFalse(evento.getFormattedMessage().contains(SENHA));
                assertFalse(evento.getFormattedMessage().contains(p.login()));
                assertFalse(evento.getFormattedMessage().contains(p.cpf()));
                assertNull(evento.getThrowableProxy());
            }
            verify(service, times(2)).criarSeNecessario(p);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void falhaDoServiceDeveInterromperRunnerSemAnunciarCriacao() {
        var service = mock(AdministradorInicialService.class);
        var p = new AdministradorInicialProperties(true, null, null, null, null, null);
        when(service.criarSeNecessario(any())).thenThrow(new IllegalArgumentException("Configuração do administrador inicial inválida"));
        assertThrows(IllegalArgumentException.class,
                () -> new AdministradorInicialRunner(service, p).run(new DefaultApplicationArguments()));
    }
}
