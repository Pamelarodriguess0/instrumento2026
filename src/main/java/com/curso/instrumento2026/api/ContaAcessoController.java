package com.curso.instrumento2026.api;

import com.curso.instrumento2026.api.dto.AlterarSenhaContaRequest;
import com.curso.instrumento2026.api.dto.ApiError;
import com.curso.instrumento2026.api.dto.ContaAcessoRequest;
import com.curso.instrumento2026.api.dto.ContaAcessoResponse;
import com.curso.instrumento2026.api.mapper.ContaAcessoMapper;
import com.curso.instrumento2026.service.ContaAcessoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/contas-acesso")
@Tag(name = "Contas de acesso")
public class ContaAcessoController {
    private final ContaAcessoService service;
    private final ContaAcessoMapper mapper;

    public ContaAcessoController(ContaAcessoService service, ContaAcessoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Operation(summary = "Cadastrar conta de acesso",
            description = "Bootstrap público didático: cria conta ATIVA com perfil OPERADOR. "
                    + "Não comprova a identidade de quem informa pessoaId; exige revisão antes de produção.")
    @SecurityRequirements
    @ApiResponse(responseCode = "201", description = "Conta cadastrada sem senha ou hash na resposta",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ContaAcessoResponse.class)))
    @ApiResponse(responseCode = "400", description = "Dados ou regra de negócio inválidos",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Pessoa não encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Login ou pessoa já vinculados a uma conta",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PostMapping
    public ResponseEntity<ContaAcessoResponse> cadastrarContaAcesso(@Valid @RequestBody ContaAcessoRequest request) {
        var conta = service.cadastrar(request.pessoaId(), request.login(), request.senha());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(conta));
    }

    @Operation(summary = "Consultar a própria conta",
            description = "Exige HTTP Basic de conta e pessoa ativas. Consulta pelo login autenticado, "
                    + "sem aceitar ID de outra conta e sem devolver senha ou hash.")
    @ApiResponse(responseCode = "200", description = "Conta autenticada sem senha ou hash",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ContaAcessoResponse.class)))
    @ApiResponse(responseCode = "401", description = "Credenciais ausentes ou inválidas, conta bloqueada ou pessoa inativa. "
            + "Resposta dos filtros de segurança, sem contrato JSON ApiError.", content = @Content)
    @ApiResponse(responseCode = "404", description = "Conta não encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/me")
    public ContaAcessoResponse consultarPropriaConta(Principal principal) {
        return mapper.toResponse(service.buscarPorLogin(principal.getName()));
    }

    @Operation(summary = "Alterar a própria senha",
            description = "Exige HTTP Basic de conta e pessoa ativas e confirmação da senha atual. "
                    + "Altera somente a conta autenticada. A nova senha exige ao menos 12 caracteres e no máximo "
                    + "72 bytes UTF-8, sem normalização. Sucesso sem corpo; a senha anterior deixa de autenticar.")
    @ApiResponse(responseCode = "204", description = "Senha alterada", content = @Content)
    @ApiResponse(responseCode = "400", description = "Senha atual ou nova senha inválidas",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Credenciais ausentes ou inválidas, conta bloqueada ou pessoa inativa. "
            + "Resposta dos filtros de segurança, sem contrato JSON ApiError.", content = @Content)
    @ApiResponse(responseCode = "404", description = "Conta não encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PutMapping("/me/senha")
    public ResponseEntity<Void> alterarPropriaSenha(Principal principal,
                                                  @Valid @RequestBody AlterarSenhaContaRequest request) {
        service.alterarSenha(principal.getName(), request.senhaAtual(), request.novaSenha());
        return ResponseEntity.noContent().build();
    }
}
