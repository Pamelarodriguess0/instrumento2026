package com.curso.instrumento2026.repository;

import com.curso.instrumento2026.domain.ContaAcesso;
import com.curso.instrumento2026.domain.Pessoa;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class ContaAcessoConsultaAdministradoresTest {
    @Test
    void consultaDoRepositoryDeveCompilarComMapeamentosReaisSemConexaoJdbc() throws Exception {
        ConnectionProvider conexoes = mock(ConnectionProvider.class);
        doThrow(new AssertionError("O teste não deve abrir conexão JDBC")).when(conexoes).getConnection();
        var registro = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", PostgreSQLDialect.class.getName())
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .applySetting("hibernate.hbm2ddl.auto", "none")
                .addService(ConnectionProvider.class, conexoes)
                .build();
        try (var factory = new MetadataSources(registro).addAnnotatedClass(ContaAcesso.class)
                .addAnnotatedClass(Pessoa.class).buildMetadata().buildSessionFactory();
             var session = factory.openSession()) {
            String jpql = ContaAcessoRepository.class.getMethod("contarAdministradoresAtivos")
                    .getAnnotation(Query.class).value();
            assertNotNull(session.createSelectionQuery(jpql, Long.class));
            verify(conexoes, never()).getConnection();
            // Only semantic query validation: no SQL is executed and no persistence result is claimed.
        } finally {
            StandardServiceRegistryBuilder.destroy(registro);
        }
    }
}
