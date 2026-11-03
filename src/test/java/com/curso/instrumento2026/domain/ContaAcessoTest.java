package com.curso.instrumento2026.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ContaAcessoTest {
    private static final String HASH_SINTETICO = "hash-sintetico-sem-credencial-real";

    private Pessoa pessoa() {
        return new Pessoa("Ana", "ana@example.com", "52998224725", LocalDate.now());
    }

    @Test
    void deveCriarContaAtivaSomenteOperadorNormalizandoLogin() {
        Pessoa pessoa = pessoa();
        var conta = new ContaAcesso(pessoa, "  ANA.Souza  ", HASH_SINTETICO);
        assertNull(conta.getId());
        assertSame(pessoa, conta.getPessoa());
        assertEquals("ana.souza", conta.getLogin());
        assertEquals(HASH_SINTETICO, conta.getSenhaHash());
        assertEquals(StatusContaAcesso.ATIVA, conta.getStatus());
        assertEquals(Set.of(PerfilAcesso.OPERADOR), conta.getPerfis());
        assertFalse(conta.possuiPerfil(PerfilAcesso.ADMINISTRADOR));
    }

    @Test
    void deveExigirPessoaSemConfundirComPapeisComerciais() {
        assertThrows(NullPointerException.class, () -> new ContaAcesso(null, "ana", HASH_SINTETICO));
        Pessoa pessoa = pessoa();
        var cliente = new Cliente(pessoa, null);
        var colaborador = new Colaborador(pessoa, "CONTA-01", FuncaoColaborador.ADMINISTRATIVO, LocalDate.now());
        var conta = new ContaAcesso(pessoa, "ana", HASH_SINTETICO);
        assertSame(cliente.getPessoa(), conta.getPessoa());
        assertSame(colaborador.getPessoa(), conta.getPessoa());
        assertEquals(Set.of(PerfilAcesso.OPERADOR), conta.getPerfis());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void deveRejeitarLoginAusente(String login) {
        assertThrows(IllegalArgumentException.class, () -> new ContaAcesso(pessoa(), login, HASH_SINTETICO));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void deveRejeitarHashAusenteSemDescartarHashAnterior(String hash) {
        assertThrows(IllegalArgumentException.class, () -> new ContaAcesso(pessoa(), "ana", hash));
        var conta = new ContaAcesso(pessoa(), "ana", HASH_SINTETICO);
        assertThrows(IllegalArgumentException.class, () -> conta.alterarSenha(hash));
        assertEquals(HASH_SINTETICO, conta.getSenhaHash());
    }

    @Test
    void deveSubstituirHashSemAlterarLoginPessoaOuPerfis() {
        Pessoa pessoa = pessoa();
        var conta = new ContaAcesso(pessoa, "ana", HASH_SINTETICO);
        conta.alterarSenha("outro-hash-sintetico");
        assertEquals("outro-hash-sintetico", conta.getSenhaHash());
        assertEquals("ana", conta.getLogin());
        assertSame(pessoa, conta.getPessoa());
        assertEquals(Set.of(PerfilAcesso.OPERADOR), conta.getPerfis());
    }

    @Test
    void deveDependerDosEstadosDaContaEDaPessoa() {
        Pessoa pessoa = pessoa();
        var conta = new ContaAcesso(pessoa, "ana", HASH_SINTETICO);
        assertTrue(conta.podeAutenticar());
        conta.bloquear();
        assertFalse(conta.podeAutenticar());
        conta.ativar();
        assertTrue(conta.podeAutenticar());
        pessoa.inativar();
        assertFalse(conta.podeAutenticar());
        conta.bloquear();
        pessoa.ativar();
        assertFalse(conta.podeAutenticar());
    }

    @Test
    void deveConcederPerfilSemDuplicarNemExporColecaoMutavel() {
        var conta = new ContaAcesso(pessoa(), "ana", HASH_SINTETICO);
        Set<PerfilAcesso> anterior = conta.getPerfis();
        conta.concederPerfil(PerfilAcesso.OPERADOR);
        conta.concederPerfil(PerfilAcesso.ADMINISTRADOR);
        assertEquals(2, conta.getPerfis().size());
        assertTrue(conta.possuiPerfil(PerfilAcesso.ADMINISTRADOR));
        assertEquals(Set.of(PerfilAcesso.OPERADOR), anterior);
        assertThrows(UnsupportedOperationException.class, () -> conta.getPerfis().clear());
        assertThrows(NullPointerException.class, () -> conta.concederPerfil(null));
    }

    @Test
    void deveValidarTodosOsPerfisAntesDeSubstituir() {
        var conta = new ContaAcesso(pessoa(), "ana", HASH_SINTETICO);
        assertThrows(IllegalArgumentException.class, () -> conta.substituirPerfis(null));
        assertThrows(IllegalArgumentException.class, () -> conta.substituirPerfis(Set.of()));
        var invalidos = new HashSet<PerfilAcesso>();
        invalidos.add(PerfilAcesso.ADMINISTRADOR);
        invalidos.add(null);
        assertThrows(IllegalArgumentException.class, () -> conta.substituirPerfis(invalidos));
        assertEquals(Set.of(PerfilAcesso.OPERADOR), conta.getPerfis());
        var novos = new HashSet<>(Set.of(PerfilAcesso.ADMINISTRADOR));
        conta.substituirPerfis(novos);
        novos.clear();
        assertEquals(Set.of(PerfilAcesso.ADMINISTRADOR), conta.getPerfis());
    }

    @Test
    void representacaoTextualNaoDeveExporHash() {
        var conta = new ContaAcesso(pessoa(), "ana", HASH_SINTETICO);
        assertFalse(conta.toString().contains(HASH_SINTETICO));
        assertFalse(conta.toString().contains("senhaHash"));
    }
}
