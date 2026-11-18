package com.curso.instrumento2026.repository;

import com.curso.instrumento2026.domain.ContaAcesso;
import com.curso.instrumento2026.domain.MotivoRevogacaoRefreshToken;
import com.curso.instrumento2026.domain.Pessoa;
import com.curso.instrumento2026.domain.SessaoRefreshToken;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class SessaoRefreshTokenConsultaTest {
    @Test
    void consultasDevemCompilarComMapeamentosReaisSemConexaoJdbc() throws Exception {
        ConnectionProvider conexoes = mock(ConnectionProvider.class);
        doThrow(new AssertionError("Este teste não pode abrir conexão JDBC")).when(conexoes).getConnection();
        var registro = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", PostgreSQLDialect.class.getName())
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .applySetting("hibernate.hbm2ddl.auto", "none")
                .addService(ConnectionProvider.class, conexoes).build();
        try (var factory = new MetadataSources(registro).addAnnotatedClass(SessaoRefreshToken.class)
                .addAnnotatedClass(ContaAcesso.class).addAnnotatedClass(Pessoa.class)
                .buildMetadata().buildSessionFactory(); var session = factory.openSession()) {
            var tipo = SessaoRefreshTokenRepository.class;
            String busca = tipo.getMethod("buscarPorHashParaAtualizacao", String.class)
                    .getAnnotation(Query.class).value();
            assertNotNull(session.createSelectionQuery(busca, SessaoRefreshToken.class));
            for (var metodo : new java.lang.reflect.Method[]{
                    tipo.getMethod("revogarFamilia", String.class, Instant.class, MotivoRevogacaoRefreshToken.class),
                    tipo.getMethod("revogarTodasDaConta", Long.class, Instant.class, MotivoRevogacaoRefreshToken.class)}) {
                assertNotNull(session.createMutationQuery(metodo.getAnnotation(Query.class).value()));
            }
            verify(conexoes, never()).getConnection();
        } finally {
            StandardServiceRegistryBuilder.destroy(registro);
        }
    }
}
