package com.curso.instrumento2026.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "venda")
public class Venda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendedor_id", nullable = false)
    private Colaborador vendedor;

    @Column(name = "data_venda", nullable = false)
    private LocalDateTime dataVenda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusVenda status;

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ItemVenda> itens = new ArrayList<>();

    protected Venda() {
    }

    public Venda(Cliente cliente, Colaborador vendedor, LocalDateTime dataVenda) {
        this.cliente = Objects.requireNonNull(cliente, "Cliente é obrigatório");
        this.vendedor = Objects.requireNonNull(vendedor, "Vendedor é obrigatório");
        this.dataVenda = Objects.requireNonNull(dataVenda, "Data da venda é obrigatória");
        this.status = StatusVenda.ABERTA;
    }

    public ItemVenda adicionarItem(Instrumento instrumento, BigDecimal quantidade) {
        validarAberta();
        Objects.requireNonNull(instrumento, "Instrumento é obrigatório");
        boolean repetido = itens.stream().anyMatch(item -> item.getInstrumento() == instrumento
                || (instrumento.getId() != null
                && Objects.equals(item.getInstrumento().getId(), instrumento.getId())));
        if (repetido) {
            throw new IllegalArgumentException("Instrumento já incluído na venda");
        }
        ItemVenda item = new ItemVenda(this, instrumento, quantidade, instrumento.getPrecoUnitario());
        itens.add(item);
        return item;
    }

    public void removerItem(Long itemId) {
        validarAberta();
        if (itemId == null || !itens.removeIf(item -> Objects.equals(item.getId(), itemId))) {
            throw new IllegalArgumentException("Item não pertence à venda");
        }
    }

    public void finalizar() {
        validarAberta();
        if (itens.isEmpty()) {
            throw new IllegalArgumentException("Venda deve possuir ao menos um item");
        }
        status = StatusVenda.FINALIZADA;
    }

    public void cancelar() {
        validarAberta();
        status = StatusVenda.CANCELADA;
    }

    private void validarAberta() {
        if (status != StatusVenda.ABERTA) {
            throw new IllegalStateException("Somente vendas abertas podem ser alteradas");
        }
    }

    public BigDecimal calcularTotal() {
        return itens.stream().map(ItemVenda::calcularSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public Colaborador getVendedor() { return vendedor; }
    public LocalDateTime getDataVenda() { return dataVenda; }
    public StatusVenda getStatus() { return status; }
    public List<ItemVenda> getItens() { return Collections.unmodifiableList(itens); }
}
