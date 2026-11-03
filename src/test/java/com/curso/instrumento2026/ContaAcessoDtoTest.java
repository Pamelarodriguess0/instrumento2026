package com.curso.instrumento2026;

import com.curso.instrumento2026.api.dto.*;
import com.curso.instrumento2026.api.mapper.ContaAcessoMapper;
import com.curso.instrumento2026.domain.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ContaAcessoDtoTest {
    private static final String SENHA = "  Frase de teste com espacos  ";
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void deveNormalizarApenasLoginEOcultarSenhasNoTextoEJson() {
        var cadastro = new ContaAcessoRequest(1L, " ANA.Loja ", SENHA);
        var troca = new AlterarSenhaContaRequest(SENHA, "Outra Frase de Teste");
        assertEquals("ana.loja", cadastro.login());
        assertEquals(SENHA, cadastro.senha());
        assertEquals(SENHA, troca.senhaAtual());
        assertFalse(cadastro.toString().contains(SENHA));
        assertFalse(troca.toString().contains(SENHA));
        assertFalse(troca.toString().contains(troca.novaSenha()));
        var cadastroJson = json.readTree(json.writeValueAsString(cadastro));
        assertFalse(cadastroJson.has("senha"));
        assertFalse(cadastroJson.has("senhaHash"));
        var trocaJson = json.readTree(json.writeValueAsString(troca));
        assertFalse(trocaJson.has("senhaAtual"));
        assertFalse(trocaJson.has("novaSenha"));
    }

    @Test
    void camposWriteOnlyDevemContinuarAceitandoEntradaSemNormalizarSenha() {
        var cadastro = json.readValue("{\"pessoaId\":1,\"login\":\" ANA.LOJA \",\"senha\":\"  Frase de teste com espacos  \"}", ContaAcessoRequest.class);
        assertEquals("ana.loja", cadastro.login());
        assertEquals(SENHA, cadastro.senha());
        var troca = json.readValue("{\"senhaAtual\":\"  Frase de teste com espacos  \",\"novaSenha\":\"Outra Frase de Teste\"}", AlterarSenhaContaRequest.class);
        assertEquals(SENHA, troca.senhaAtual());
        assertEquals("Outra Frase de Teste", troca.novaSenha());
    }

    @Test
    void respostaDeveConterSomenteDadosPublicosSemHashOuSenha() {
        var pessoa = new Pessoa("Ana", "ana@example.com", "52998224725", LocalDate.now());
        var conta = new ContaAcesso(pessoa, "ana", "hash-sintetico-que-nao-deve-sair");
        var resposta = new ContaAcessoMapper().toResponse(conta);
        assertEquals(Set.of("id", "pessoaId", "nomePessoa", "login", "status", "perfis"),
                Arrays.stream(ContaAcessoResponse.class.getRecordComponents()).map(c -> c.getName()).collect(Collectors.toSet()));
        String serializada = json.writeValueAsString(resposta);
        assertFalse(serializada.contains("senha"));
        assertFalse(serializada.contains(conta.getSenhaHash()));
        assertFalse(resposta.toString().contains(conta.getSenhaHash()));
        assertEquals(Set.of(PerfilAcesso.OPERADOR), resposta.perfis());
        assertThrows(UnsupportedOperationException.class, () -> resposta.perfis().clear());
    }

    @Test
    void deveValidarDtosSemMensagensQueIncluamCredenciais() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(new ContaAcessoRequest(1L, " ANA.LOJA ", SENHA)).isEmpty());
            assertEquals(Set.of("pessoaId", "login", "senha"), validator.validate(new ContaAcessoRequest(null, "!", "curta"))
                    .stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet()));
            assertFalse(validator.validate(new AlterarSenhaContaRequest("", "curta")).isEmpty());
            assertFalse(validator.validate(new StatusContaAcessoRequest(null)).isEmpty());
            assertTrue(validator.validate(new StatusContaAcessoRequest(StatusContaAcesso.BLOQUEADA)).isEmpty());
            assertTrue(validator.validate(new ContaAcessoRequest(1L, "operador", "curta")).stream()
                    .noneMatch(v -> v.getMessage().contains("curta")));
        }
    }
}
