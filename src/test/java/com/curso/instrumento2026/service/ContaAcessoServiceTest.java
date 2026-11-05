package com.curso.instrumento2026.service;

import org.springframework.security.test.context.support.WithMockUser;

import com.curso.instrumento2026.api.mapper.ContaAcessoMapper;
import com.curso.instrumento2026.domain.*;
import com.curso.instrumento2026.exception.RecursoDuplicadoException;
import com.curso.instrumento2026.exception.RecursoNaoEncontradoException;
import com.curso.instrumento2026.repository.ContaAcessoRepository;
import com.curso.instrumento2026.repository.PessoaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// Sem @Transactional no teste: verificar os dados confirmados ou revertidos pelo service.
@WithMockUser(roles = "ADMINISTRADOR")
@SpringBootTest
@ActiveProfiles("test")
class ContaAcessoServiceTest {
    private static final String SENHA = "  Minha Frase Secreta 2026  ";
    private static final String NOVA = "Outra Frase Secreta 2026";
    @Autowired ContaAcessoService service;
    @Autowired ContaAcessoRepository contas;
    @Autowired PessoaRepository pessoas;
    @Autowired PasswordEncoder encoder;
    @Autowired ContaAcessoMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    private Long pessoaId;
    private Long outraPessoaId;

    @BeforeEach
    void preparar() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            String execucao = UUID.randomUUID().toString();
            pessoaId = pessoas.save(new Pessoa("Pessoa acesso service", execucao + "@example.com",
                    "52998224725", LocalDate.now())).getId();
            outraPessoaId = pessoas.save(new Pessoa("Outra pessoa acesso", execucao + ".outra@example.com",
                    "11144477735", LocalDate.now())).getId();
        });
    }

    @AfterEach
    void limpar() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("DELETE FROM conta_acesso WHERE pessoa_id IN (?,?)", pessoaId, outraPessoaId);
            jdbc.update("DELETE FROM pessoa WHERE id IN (?,?)", pessoaId, outraPessoaId);
        });
    }

    private void alterarPessoa(Status novoStatus) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Pessoa pessoa = pessoas.findById(pessoaId).orElseThrow();
            if (novoStatus == Status.ATIVO) pessoa.ativar(); else pessoa.inativar();
        });
    }

    @Test
    void deveCadastrarContaComBCryptPersistidoESenhaPreservadaIntegralmente() {
        var conta = service.cadastrar(pessoaId, "  OPERADOR.Loja  ", SENHA);
        var salva = service.buscarPorId(conta.getId());
        String hash = jdbc.queryForObject("SELECT senha_hash FROM conta_acesso WHERE id=?", String.class, conta.getId());
        assertNotEquals(SENHA, hash);
        assertTrue(hash.startsWith("{bcrypt}"));
        assertTrue(encoder.matches(SENHA, hash));
        assertFalse(encoder.matches(SENHA.trim(), hash));
        assertFalse(encoder.matches(SENHA.toLowerCase(), hash));
        assertEquals(hash, salva.getSenhaHash());
        assertEquals("operador.loja", salva.getLogin());
        assertEquals(pessoaId, salva.getPessoa().getId());
        assertEquals(StatusContaAcesso.ATIVA, salva.getStatus());
        assertEquals(Set.of(PerfilAcesso.OPERADOR), salva.getPerfis());
        assertTrue(salva.podeAutenticar());
        // O mapper funciona também após o encerramento da transação de consulta.
        assertEquals("Pessoa acesso service", mapper.toResponse(salva).nomePessoa());
    }

    @Test
    void senhasIguaisDevemGerarHashesDiferentesPorSalt() {
        var primeira = service.cadastrar(pessoaId, "operador.a", SENHA);
        var segunda = service.cadastrar(outraPessoaId, "operador.b", SENHA);
        assertNotEquals(primeira.getSenhaHash(), segunda.getSenhaHash());
        assertTrue(encoder.matches(SENHA, primeira.getSenhaHash()));
        assertTrue(encoder.matches(SENHA, segunda.getSenhaHash()));
    }

    @Test
    void deveRejeitarLoginDuplicadoSemExporCredenciais() {
        service.cadastrar(pessoaId, "operador.loja", SENHA);
        var erro = assertThrows(RecursoDuplicadoException.class,
                () -> service.cadastrar(outraPessoaId, " OPERADOR.LOJA ", NOVA));
        assertEquals("Login já cadastrado", erro.getMessage());
        assertFalse(erro.toString().contains(NOVA));
        assertFalse(contas.existsByPessoaId(outraPessoaId));
    }

    @Test
    void deveRejeitarSegundaContaDaPessoa() {
        service.cadastrar(pessoaId, "primeiro.login", SENHA);
        assertEquals("Pessoa já possui conta de acesso", assertThrows(RecursoDuplicadoException.class,
                () -> service.cadastrar(pessoaId, "segundo.login", NOVA)).getMessage());
        assertFalse(contas.existsByLoginIgnoreCase("segundo.login"));
    }

    @Test
    void deveRejeitarPessoaAusenteOuInexistente() {
        assertEquals("Pessoa é obrigatória", assertThrows(IllegalArgumentException.class,
                () -> service.cadastrar(null, "operador", SENHA)).getMessage());
        assertEquals("Pessoa não encontrada", assertThrows(RecursoNaoEncontradoException.class,
                () -> service.cadastrar(Long.MAX_VALUE, "operador", SENHA)).getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "ab", "nome com espaco", "operador@loja", "ação"})
    void deveRejeitarLoginInvalidoSemPersistir(String login) {
        assertThrows(IllegalArgumentException.class, () -> service.cadastrar(pessoaId, login, SENHA));
        assertFalse(contas.existsByPessoaId(pessoaId));
    }

    @Test
    void deveRespeitarLimitesDeTamanhoDoLogin() {
        assertThrows(IllegalArgumentException.class, () -> service.cadastrar(pessoaId, "a".repeat(81), SENHA));
        assertEquals(80, service.cadastrar(pessoaId, "a".repeat(80), SENHA).getLogin().length());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"            ", "curta", "12345678901"})
    void deveRejeitarSenhaAusenteOuCurtaSemExporSeuConteudo(String senha) {
        var erro = assertThrows(IllegalArgumentException.class, () -> service.cadastrar(pessoaId, "operador", senha));
        assertTrue(erro.getMessage().equals("Senha é obrigatória")
                || erro.getMessage().equals("Senha deve possuir ao menos 12 caracteres"));
        assertFalse(contas.existsByPessoaId(pessoaId));
    }

    @Test
    void deveRejeitarSenhaMaiorQue72BytesInclusiveMultibyte() {
        for (String senha : new String[]{"a".repeat(73), "é".repeat(37), "🔒".repeat(19)}) {
            assertEquals("Senha deve possuir no máximo 72 bytes em UTF-8",
                    assertThrows(IllegalArgumentException.class,
                            () -> service.cadastrar(pessoaId, "operador", senha)).getMessage());
        }
        assertFalse(contas.existsByPessoaId(pessoaId));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456789012", "áéíóúçãõÁÉÍÓ", "123456789012345678901234567890123456789012345678901234567890123456789012"})
    void deveAceitarSenhaValidaSemNormalizar(String senha) {
        var conta = service.cadastrar(pessoaId, "operador", senha);
        assertTrue(encoder.matches(senha, service.buscarPorId(conta.getId()).getSenhaHash()));
    }

    @Test
    void deveAceitarExatamente72BytesMultibyteEExigir12CaracteresUnicode() {
        String senha = "é".repeat(36);
        assertTrue(encoder.matches(senha, service.cadastrar(pessoaId, "operador", senha).getSenhaHash()));
        assertEquals("Senha deve possuir ao menos 12 caracteres", assertThrows(IllegalArgumentException.class,
                () -> service.cadastrar(outraPessoaId, "outro.operador", "🔒".repeat(11))).getMessage());
    }

    @Test
    void deveAlterarSenhaComCommitInvalidandoAnteriorEPreservandoConta() {
        var conta = service.cadastrar(pessoaId, "operador", SENHA);
        service.alterarSenha(" OPERADOR ", SENHA, NOVA);
        var salva = service.buscarPorLogin("operador");
        assertEquals(conta.getId(), salva.getId());
        assertTrue(salva.getSenhaHash().startsWith("{bcrypt}"));
        assertTrue(encoder.matches(NOVA, salva.getSenhaHash()));
        assertFalse(encoder.matches(SENHA, salva.getSenhaHash()));
        assertEquals(Set.of(PerfilAcesso.OPERADOR), salva.getPerfis());
        assertEquals(StatusContaAcesso.ATIVA, salva.getStatus());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"errada", "            "})
    void deveRejeitarSenhaAtualInvalidaSemModificarHash(String atual) {
        var conta = service.cadastrar(pessoaId, "operador", SENHA);
        var erro = assertThrows(IllegalArgumentException.class, () -> service.alterarSenha("operador", atual, NOVA));
        assertEquals("Senha atual inválida", erro.getMessage());
        assertFalse(erro.toString().contains(NOVA));
        assertEquals(conta.getSenhaHash(), service.buscarPorId(conta.getId()).getSenhaHash());
    }

    @Test
    void deveRejeitarNovaSenhaReutilizadaCurtaOuLongaSemModificarHash() {
        var conta = service.cadastrar(pessoaId, "operador", SENHA);
        assertEquals("Nova senha deve ser diferente da senha atual", assertThrows(IllegalArgumentException.class,
                () -> service.alterarSenha("operador", SENHA, SENHA)).getMessage());
        for (String nova : new String[]{"curta", "é".repeat(37)}) {
            assertThrows(IllegalArgumentException.class, () -> service.alterarSenha("operador", SENHA, nova));
        }
        assertThrows(IllegalArgumentException.class, () -> service.alterarSenha("operador", "x".repeat(73), NOVA));
        assertEquals(conta.getSenhaHash(), service.buscarPorId(conta.getId()).getSenhaHash());
    }

    @Test
    void deveBloquearEDesbloquearPreservandoHashPerfisEPessoa() {
        var conta = service.cadastrar(pessoaId, "operador", SENHA);
        service.alterarStatus(conta.getId(), StatusContaAcesso.BLOQUEADA);
        assertFalse(service.buscarPorId(conta.getId()).podeAutenticar());
        service.alterarStatus(conta.getId(), StatusContaAcesso.ATIVA);
        var salva = service.buscarPorId(conta.getId());
        assertTrue(salva.podeAutenticar());
        assertEquals(conta.getSenhaHash(), salva.getSenhaHash());
        assertEquals(Set.of(PerfilAcesso.OPERADOR), salva.getPerfis());
        assertEquals(pessoaId, salva.getPessoa().getId());
    }

    @Test
    void deveRejeitarCadastroDePessoaInativaEManterDependenciaDoStatusDaPessoa() {
        alterarPessoa(Status.INATIVO);
        assertEquals("Pessoa deve estar ativa", assertThrows(IllegalArgumentException.class,
                () -> service.cadastrar(pessoaId, "operador", SENHA)).getMessage());
        alterarPessoa(Status.ATIVO);
        var conta = service.cadastrar(pessoaId, "operador", SENHA);
        alterarPessoa(Status.INATIVO);
        service.alterarStatus(conta.getId(), StatusContaAcesso.BLOQUEADA);
        service.alterarStatus(conta.getId(), StatusContaAcesso.ATIVA);
        assertFalse(service.buscarPorLogin("operador").podeAutenticar());
        assertEquals(Status.INATIVO, pessoas.findById(pessoaId).orElseThrow().getStatus());
    }

    @Test
    void deveRejeitarStatusNuloEContaInexistente() {
        assertThrows(IllegalArgumentException.class, () -> service.alterarStatus(1L, null));
        assertThrows(IllegalArgumentException.class, () -> service.buscarPorId(null));
        assertThrows(RecursoNaoEncontradoException.class, () -> service.buscarPorId(Long.MAX_VALUE));
        assertThrows(RecursoNaoEncontradoException.class, () -> service.buscarPorLogin("inexistente"));
        assertThrows(RecursoNaoEncontradoException.class, () -> service.alterarStatus(Long.MAX_VALUE, StatusContaAcesso.ATIVA));
        assertThrows(RecursoNaoEncontradoException.class, () -> service.alterarSenha("inexistente", SENHA, NOVA));
    }
}
