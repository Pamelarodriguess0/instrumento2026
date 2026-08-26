package com.curso.instrumento2026.domain;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "categoria_instrumento")
public class CategoriaInstrumento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @OneToMany(mappedBy = "categoria")
    private List<Instrumento> instrumentos = new ArrayList<>();

    protected CategoriaInstrumento() {
    }

    public CategoriaInstrumento(Long id, String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome da categoria é obrigatório");
        }
        this.id = id;
        this.nome = nome;
        this.status =Status.ATIVO;
    }

    public void adicionarInstrumento(Instrumento instrumento) {
        if (instrumento == null) {
            throw new IllegalArgumentException("Instrumento é obrigatório");
        }

        if (instrumento.getCategoria() != this){
            throw new IllegalArgumentException("Instrumento pertence a outra categoria ");
        }

        if (instrumentos.contains(instrumento)) {
            return;
        }
        instrumentos.add(instrumento);
    }

    public List<Instrumento> getInstrumentos() {
        return Collections.unmodifiableList(instrumentos);
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }


}
