package com.curso.instrumento2026.service;

import com.curso.instrumento2026.config.AdministradorInicialProperties;
import com.curso.instrumento2026.domain.ContaAcesso;
import com.curso.instrumento2026.domain.PerfilAcesso;
import com.curso.instrumento2026.domain.Pessoa;
import com.curso.instrumento2026.exception.RecursoDuplicadoException;
import com.curso.instrumento2026.repository.ContaAcessoRepository;
import com.curso.instrumento2026.repository.PessoaRepository;
import jakarta.validation.Validator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static com.curso.instrumento2026.service.ValidacaoCredenciais.normalizarLogin;
import static com.curso.instrumento2026.service.ValidacaoCredenciais.validarSenha;

@Service
public class AdministradorInicialService {
    private final ContaAcessoRepository contas;
    private final PessoaRepository pessoas;
    private final PasswordEncoder encoder;
    private final Validator validator;

    public AdministradorInicialService(ContaAcessoRepository contas, PessoaRepository pessoas,
                                      PasswordEncoder encoder, Validator validator) {
        this.contas = contas;
        this.pessoas = pessoas;
        this.encoder = encoder;
        this.validator = validator;
    }

    @Transactional
    public Optional<ContaAcesso> criarSeNecessario(AdministradorInicialProperties properties) {
        if (!properties.enabled()) return Optional.empty();
        if (contas.contarAdministradoresAtivos() > 0) return Optional.empty();
        if (!validator.validate(properties).isEmpty()) {
            // Do not include ConstraintViolation objects: their rendering may expose rejected credentials.
            throw new IllegalArgumentException("Configuração do administrador inicial inválida");
        }
        String login = normalizarLogin(properties.login());
        validarSenha(properties.senha());
        Pessoa pessoa = new Pessoa(properties.nome(), properties.email(), properties.cpf(), LocalDate.now());
        if (contas.existsByLoginIgnoreCase(login)) throw new RecursoDuplicadoException("Login já cadastrado");
        if (pessoas.existsByCpf(pessoa.getCpf())) throw new RecursoDuplicadoException("CPF já cadastrado");
        if (pessoas.existsByEmailIgnoreCase(pessoa.getEmail())) throw new RecursoDuplicadoException("E-mail já cadastrado");

        String hash = encoder.encode(properties.senha());
        try {
            Pessoa salva = pessoas.save(pessoa);
            ContaAcesso conta = new ContaAcesso(salva, login, hash);
            conta.concederPerfil(PerfilAcesso.ADMINISTRADOR);
            return Optional.of(contas.saveAndFlush(conta));
        } catch (DataIntegrityViolationException exception) {
            // Keep the exception escaping the transaction, without logging SQL/cause details during startup.
            throw new IllegalStateException("Não foi possível provisionar o administrador inicial");
        }
    }
}
