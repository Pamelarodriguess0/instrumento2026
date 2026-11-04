package com.curso.instrumento2026.api;

import com.curso.instrumento2026.api.dto.LoginRequest;
import com.curso.instrumento2026.api.dto.TokenResponse;
import com.curso.instrumento2026.service.AutenticacaoService;
import jakarta.validation.Valid;
import com.curso.instrumento2026.api.dto.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/autenticacao")
public class AutenticacaoController {
    private final AutenticacaoService service;

    public AutenticacaoController(AutenticacaoService service) {
        this.service = service;
    }

    @Operation(summary = "Autenticar e emitir JWT",
            description = "Operação pública. Autentica login e senha e emite JWT HS256 válido por 30 minutos. "
                    + "Conta e pessoa devem estar ativas no login. Envie somente o accessToken no Authorize do Swagger; "
                    + "o esquema acrescenta Bearer. Credenciais inválidas recebem mensagem genérica em ApiError. "
                    + "O token não contém senha ou hash e permanece válido até expirar, sem revogação nesta aula.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Token emitido",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenResponse.class)))
    @ApiResponse(responseCode = "400", description = "JSON ou dados de entrada inválidos",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Login ou senha inválidos. Erro da autenticação no login em ApiError; "
            + "não distingue conta inexistente, senha incorreta, bloqueio ou pessoa inativa. "
            + "Bearer inválido enviado ao endpoint pode ser rejeitado antes pelos filtros, sem este contrato JSON.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        var token = service.autenticar(request.login(), request.senha());
        return new TokenResponse(token.valor(), "Bearer", token.duracaoSegundos(), token.expiraEm());
    }
}
