package com.curso.instrumento2026.service;

import com.curso.instrumento2026.config.RefreshTokenProperties;
import com.curso.instrumento2026.domain.*;
import com.curso.instrumento2026.exception.RefreshTokenInvalidoException;
import com.curso.instrumento2026.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessaoAutenticacaoServiceTest {
    static final Instant AGORA = Instant.parse("2026-11-17T12:00:00Z");
    SessaoRefreshTokenRepository sessoes = mock(SessaoRefreshTokenRepository.class);
    ContaAcessoRepository contas = mock(ContaAcessoRepository.class);
    TokenService tokens = mock(TokenService.class);
    ContaAcesso conta;
    SessaoAutenticacaoService service;
    Authentication auth;
    @BeforeEach void configurar() {
        conta = new ContaAcesso(new Pessoa("Pessoa", "pessoa@example.com", "52998224725", LocalDate.of(2026,11,17)), "operador", "hash-sintetico");
        service = new SessaoAutenticacaoService(sessoes, contas, tokens,
                new RefreshTokenProperties(Duration.ofMinutes(30), Duration.ofHours(8)),
                Clock.fixed(AGORA, ZoneOffset.UTC), mock(EntityManager.class), mock(AuditoriaSegurancaService.class));
        auth = UsernamePasswordAuthenticationToken.authenticated("operador", null, List.of());
        when(contas.findByLoginIgnoreCase("operador")).thenReturn(Optional.of(conta));
        when(tokens.emitir(any(), anyString())).thenReturn(new TokenAcesso("access-sintetico", AGORA.plusSeconds(1800),1800));
    }
    @Test void deveEmitirAleatorioArmazenandoSomenteHash() throws Exception {
        var par = service.iniciar(auth);
        var outro = service.iniciar(auth);
        assertEquals(32, Base64.getUrlDecoder().decode(par.refreshToken()).length);
        assertTrue(par.refreshToken().matches("[A-Za-z0-9_-]{43}"));
        assertNotEquals(par.refreshToken(), outro.refreshToken());
        var captor = org.mockito.ArgumentCaptor.forClass(SessaoRefreshToken.class);
        verify(sessoes,times(2)).save(captor.capture());
        var primeira = captor.getAllValues().getFirst();
        assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(par.refreshToken().getBytes(StandardCharsets.UTF_8))), primeira.getTokenHash());
        assertNotEquals(primeira.getFamiliaId(), captor.getAllValues().getLast().getFamiliaId());
        assertFalse(par.toString().contains(par.refreshToken()));
        assertFalse(par.toString().contains("access-sintetico"));
        assertEquals(AGORA.plusSeconds(28800),par.refreshTokenExpiresAt());
    }
    @Test void deveRotacionarPreservandoFamiliaLimiteERecarregandoPerfis() {
        var sessao = sessao(AGORA.minusSeconds(60), AGORA.plusSeconds(120));
        conta.concederPerfil(PerfilAcesso.ADMINISTRADOR);
        when(sessoes.buscarPorHashParaAtualizacao(anyString())).thenReturn(Optional.of(sessao));
        var par = service.renovar("refresh-sintetico");
        assertEquals(AGORA, sessao.getUtilizadaEm());
        assertEquals(sessao.getExpiraEm(), par.refreshTokenExpiresAt());
        var captor = org.mockito.ArgumentCaptor.forClass(SessaoRefreshToken.class);
        verify(sessoes).save(captor.capture());
        assertEquals(sessao.getFamiliaId(),captor.getValue().getFamiliaId());
        var authentication = org.mockito.ArgumentCaptor.forClass(Authentication.class);
        verify(tokens).emitir(authentication.capture(),eq(sessao.getFamiliaId()));
        assertTrue(authentication.getValue().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR")));
    }
    @Test void deveRejeitarAusenteEDesconhecidoSemEscritas() {
        for (String valor : new String[]{null,"", " ", "desconhecido"})
            assertThrows(RefreshTokenInvalidoException.class, () -> service.renovar(valor));
        verify(sessoes,never()).save(any());
        verify(sessoes,never()).revogarFamilia(any(),any(),any());
    }
    @ParameterizedTest @EnumSource(value=MotivoRevogacaoRefreshToken.class, names={"REUTILIZACAO","INATIVIDADE","EXPIRACAO_ABSOLUTA","CONTA_INATIVA"})
    void deveRevogarFamiliaPorRejeicaoPrevista(MotivoRevogacaoRefreshToken motivo) {
        var sessao = sessao(AGORA.minusSeconds(motivo==MotivoRevogacaoRefreshToken.INATIVIDADE?1800:60),
                motivo==MotivoRevogacaoRefreshToken.EXPIRACAO_ABSOLUTA?AGORA:AGORA.plusSeconds(3600));
        if (motivo==MotivoRevogacaoRefreshToken.REUTILIZACAO) sessao.marcarUtilizada(AGORA.minusSeconds(1));
        if (motivo==MotivoRevogacaoRefreshToken.CONTA_INATIVA) conta.bloquear();
        when(sessoes.buscarPorHashParaAtualizacao(any())).thenReturn(Optional.of(sessao));
        assertThrows(RefreshTokenInvalidoException.class,()->service.renovar("sintetico"));
        verify(sessoes).revogarFamilia(sessao.getFamiliaId(),AGORA,motivo);
        verify(sessoes,never()).save(any());
    }
    @Test void deveRejeitarRevogadaPreservandoMotivo() {
        var sessao=sessao(AGORA.minusSeconds(60),AGORA.plusSeconds(3600));
        sessao.revogar(AGORA.minusSeconds(1),MotivoRevogacaoRefreshToken.LOGOUT);
        when(sessoes.buscarPorHashParaAtualizacao(any())).thenReturn(Optional.of(sessao));
        assertThrows(RefreshTokenInvalidoException.class,()->service.renovar("sintetico"));
        verify(sessoes,never()).revogarFamilia(any(),any(),any());
    }
    private SessaoRefreshToken sessao(Instant criacao,Instant limite) {
        return new SessaoRefreshToken(conta,UUID.randomUUID().toString(),"a".repeat(64),criacao,criacao,limite);
    }
}
