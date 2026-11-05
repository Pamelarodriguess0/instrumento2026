package com.curso.instrumento2026.api;

import com.curso.instrumento2026.api.dto.AlterarSenhaContaRequest;
import com.curso.instrumento2026.api.dto.ApiError;
import com.curso.instrumento2026.api.dto.ContaAcessoRequest;
import com.curso.instrumento2026.api.dto.ContaAcessoResponse;
import com.curso.instrumento2026.api.dto.PerfisContaAcessoRequest;
import com.curso.instrumento2026.api.dto.StatusContaAcessoRequest;
import com.curso.instrumento2026.api.mapper.ContaAcessoMapper;
import com.curso.instrumento2026.service.ContaAcessoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
            description = "Cria conta ATIVA com perfil OPERADOR. Operação administrativa; "
                    + "não permite escolher perfis ou status no cadastro.")
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
            description = "Exige Bearer JWT válido. Consulta pelo login do subject autenticado, "
                    + "sem aceitar ID de outra conta e sem devolver senha ou hash.")
    @ApiResponse(responseCode = "200", description = "Conta autenticada sem senha ou hash",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ContaAcessoResponse.class)))
    @ApiResponse(responseCode = "401", description = "Bearer ausente, inválido ou expirado. "
            + "Resposta dos filtros de segurança, sem contrato JSON ApiError.", content = @Content)
    @ApiResponse(responseCode = "404", description = "Conta não encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @GetMapping("/me")
    public ContaAcessoResponse consultarPropriaConta(Principal principal) {
        return mapper.toResponse(service.buscarPorLogin(principal.getName()));
    }

    @Operation(summary = "Alterar a própria senha",
            description = "Exige Bearer JWT válido e confirmação da senha atual. "
                    + "Altera somente a conta autenticada. A nova senha exige ao menos 12 caracteres e no máximo "
                    + "72 bytes UTF-8, sem normalização. Sucesso sem corpo; a senha anterior deixa de permitir novos logins. Tokens já emitidos não são revogados.")
    @ApiResponse(responseCode = "204", description = "Senha alterada", content = @Content)
    @ApiResponse(responseCode = "400", description = "Senha atual ou nova senha inválidas",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Bearer ausente, inválido ou expirado. "
            + "Resposta dos filtros de segurança, sem contrato JSON ApiError.", content = @Content)
    @ApiResponse(responseCode = "404", description = "Conta não encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PutMapping("/me/senha")
    public ResponseEntity<Void> alterarPropriaSenha(Principal principal,
                                                  @Valid @RequestBody AlterarSenhaContaRequest request) {
        service.alterarSenha(principal.getName(), request.senhaAtual(), request.novaSenha());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Alterar perfis da conta",
            description = "Substitui os perfis da conta por um conjunto não vazio de OPERADOR e/ou ADMINISTRADOR. "
                    + "Impede retirar o próprio perfil administrativo e remover o último administrador utilizável "
                    + "(conta ATIVA e Pessoa ATIVA). Não revoga nem altera tokens já emitidos; novos perfis exigem novo login.")
    @ApiResponse(responseCode = "200", description = "Perfis atualizados, sem senha ou hash",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ContaAcessoResponse.class)))
    @ApiResponse(responseCode = "400", description = "Perfis inválidos, autorremoção administrativa ou último administrador utilizável",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Conta não encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PutMapping("/{id}/perfis")
    public ContaAcessoResponse alterarPerfis(@PathVariable Long id,
                                           @Valid @RequestBody PerfisContaAcessoRequest request) {
        return mapper.toResponse(service.alterarPerfis(id, request.perfis()));
    }

    @Operation(operationId = "alterarStatusContaAcesso", summary = "Alterar status da conta",
            description = "Ativa ou bloqueia outra conta. Impede autobloqueio e bloqueio do último administrador utilizável "
                    + "(conta ATIVA e Pessoa ATIVA). Bloqueio impede novos logins, sem revogar tokens já emitidos.")
    @ApiResponse(responseCode = "200", description = "Status atualizado, sem senha ou hash",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ContaAcessoResponse.class)))
    @ApiResponse(responseCode = "400", description = "Status inválido, autobloqueio ou último administrador utilizável",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Conta não encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PutMapping("/{id}/status")
    public ContaAcessoResponse alterarStatus(@PathVariable Long id,
                                           @Valid @RequestBody StatusContaAcessoRequest request) {
        return mapper.toResponse(service.alterarStatus(id, request.status()));
    }
}
