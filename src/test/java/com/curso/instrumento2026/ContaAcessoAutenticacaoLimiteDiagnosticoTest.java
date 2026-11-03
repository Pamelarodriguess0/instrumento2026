package com.curso.instrumento2026;

import com.curso.instrumento2026.domain.Pessoa;
import com.curso.instrumento2026.repository.PessoaRepository;
import com.curso.instrumento2026.service.ContaAcessoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

// Diagnóstico com filtros reais: manter a rejeição de 73 bytes como contrato, mesmo se falhar.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContaAcessoAutenticacaoLimiteDiagnosticoTest {
    @Autowired MockMvc mvc;
    @Autowired PessoaRepository pessoas;
    @Autowired ContaAcessoService contas;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    private Long pessoaId;
    private String login;

    @BeforeEach
    void preparar() {
        String token = UUID.randomUUID().toString();
        login = "limite.http." + token;
        pessoaId = new TransactionTemplate(transactionManager).execute(status -> pessoas.save(
                new Pessoa("Diagnostico limite HTTP", token + "@example.com", "52998224725", LocalDate.now())).getId());
    }

    @AfterEach
    void limpar() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("DELETE FROM conta_acesso WHERE pessoa_id=?", pessoaId);
            jdbc.update("DELETE FROM pessoa WHERE id=?", pessoaId);
        });
    }

    @ParameterizedTest(name = "Basic deve rejeitar 73 bytes; multibyte={0}")
    @ValueSource(booleans = {false, true})
    void deveRejeitarSenhaAcimaDe72BytesNoHttpBasic(boolean multibyte) throws Exception {
        String correta = multibyte ? "é".repeat(36) : "a".repeat(72);
        String excedente = correta + "X";
        String incorreta = "z".repeat(72);
        assertEquals(72, correta.getBytes(StandardCharsets.UTF_8).length);
        assertEquals(73, excedente.getBytes(StandardCharsets.UTF_8).length);
        Long contaId = contas.cadastrar(pessoaId, login, correta).getId();
        // Cadastro confirmado no banco; não é apenas um experimento de PasswordEncoder.
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM conta_acesso WHERE id=?", Integer.class, contaId));

        var original = mvc.perform(get("/api/contas-acesso/me").with(httpBasic(login, correta))).andReturn();
        var acimaDoLimite = mvc.perform(get("/api/contas-acesso/me").with(httpBasic(login, excedente))).andReturn();
        var errada = mvc.perform(get("/api/contas-acesso/me").with(httpBasic(login, incorreta))).andReturn();
        int statusOriginal = original.getResponse().getStatus();
        int statusExcedente = acimaDoLimite.getResponse().getStatus();
        int statusErrada = errada.getResponse().getStatus();
        System.out.printf("DIAGNOSTICO BASIC: multibyte=%s, bytesOriginal=72, bytesExcedente=73, "
                        + "original=%d, excedente=%d, incorreta=%d%n",
                multibyte, statusOriginal, statusExcedente, statusErrada);

        assertAll("Credenciais originais aceitas e credenciais diferentes rejeitadas",
                () -> assertEquals(200, statusOriginal, "Senha correta de 72 bytes deve autenticar"),
                () -> assertEquals(401, statusExcedente, "Senha de 73 bytes deve ser rejeitada mesmo com prefixo igual"),
                () -> assertEquals(401, statusErrada, "Senha incorreta deve ser rejeitada"));
    }
}
