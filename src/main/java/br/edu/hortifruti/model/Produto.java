package br.edu.hortifruti.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Entity
@Table(name = "produtos")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Informe o nome do produto.")
    private String nome;

    @Column(nullable = false)
    @NotBlank(message = "Informe a categoria do produto.")
    private String categoria;

    @Column(nullable = false)
    @NotNull(message = "Informe a quantidade disponível.")
    @PositiveOrZero(message = "A quantidade não pode ser negativa.")
    private Double quantidade;

    @Column(nullable = false)
    @NotBlank(message = "Informe a unidade de medida.")
    private String unidade;

    @Column(nullable = false, columnDefinition = "DOUBLE DEFAULT 5.0")
    @NotNull(message = "Informe o estoque mínimo.")
    @PositiveOrZero(message = "O estoque mínimo não pode ser negativo.")
    private Double estoqueMinimo = 5.0;

    public Produto() {
    }

    public Produto(String nome, String categoria, Double quantidade, String unidade) {
        this.nome = nome;
        this.categoria = categoria;
        this.quantidade = quantidade;
        this.unidade = unidade;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Double getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Double quantidade) {
        this.quantidade = quantidade;
    }

    public String getUnidade() {
        return unidade;
    }

    public void setUnidade(String unidade) {
        this.unidade = unidade;
    }

    public Double getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public void setEstoqueMinimo(Double estoqueMinimo) {
        this.estoqueMinimo = estoqueMinimo;
    }
}
