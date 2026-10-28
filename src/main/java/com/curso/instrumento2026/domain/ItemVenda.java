package com.curso.instrumento2026.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Entity
@Table(name = "item_venda", uniqueConstraints = @UniqueConstraint(
        name = "uk_item_venda_instrumento", columnNames = {"venda_id", "instrumento_id"}))
public class ItemVenda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venda_id", nullable = false)
    private Venda venda;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrumento_id", nullable = false)
    private Instrumento instrumento;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal quantidade;

    @Column(name = "valor_unitario", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorUnitario;

    protected ItemVenda() {
    }

    ItemVenda(Venda venda, Instrumento instrumento, BigDecimal quantidade, BigDecimal valorUnitario) {
        this.venda = Objects.requireNonNull(venda, "Venda é obrigatória");
        this.instrumento = Objects.requireNonNull(instrumento, "Instrumento é obrigatório");
        this.quantidade = validarQuantidade(quantidade);
        this.valorUnitario = validarValorUnitario(valorUnitario);
    }

    private static BigDecimal validarQuantidade(BigDecimal quantidade) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }
        try {
            quantidade.intValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Quantidade deve ser inteira e estar no intervalo permitido", exception);
        }
        return quantidade;
    }

    private static BigDecimal validarValorUnitario(BigDecimal valor) {
        if (valor == null || valor.signum() < 0) {
            throw new IllegalArgumentException("Valor unitário não pode ser negativo");
        }
        return valor;
    }

    public BigDecimal calcularSubtotal() {
        return quantidade.multiply(valorUnitario).setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public Venda getVenda() { return venda; }
    public Instrumento getInstrumento() { return instrumento; }
    public BigDecimal getQuantidade() { return quantidade; }
    public BigDecimal getValorUnitario() { return valorUnitario; }
}
