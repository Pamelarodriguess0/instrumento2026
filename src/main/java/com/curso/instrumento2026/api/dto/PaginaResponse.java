
package com.curso.instrumento2026.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

@Schema(description = "Resposta paginada contendo os resultados da pesquisa e informações de paginação")
public record PaginaResponse<T>(

        @Schema(description = "Lista de registros encontrados na página")
        List<T> conteudo,

        @Schema(description = "Número da página atual, começando em zero", example = "0")
        int pagina,

        @Schema(description = "Quantidade de registros por página", example = "20")
        int tamanho,

        @Schema(description = "Quantidade total de registros encontrados", example = "45")
        long totalElementos,

        @Schema(description = "Quantidade total de páginas", example = "3")
        int totalPaginas,

        @Schema(description = "Indica se esta é a primeira página", example = "true")
        boolean primeira,

        @Schema(description = "Indica se esta é a última página", example = "false")
        boolean ultima

) {

    public static <S, T> PaginaResponse<T> de(
            Page<S> pagina,
            Function<S, T> conversor) {

        return new PaginaResponse<>(
                pagina.getContent().stream().map(conversor).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.isFirst(),
                pagina.isLast()
        );
    }
}
