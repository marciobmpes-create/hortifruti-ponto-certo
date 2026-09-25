package br.edu.hortifruti.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "movimentacoes")
public class Movimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne
    @JoinColumn(name = "lote_id")
    private Lote lote;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private Double quantidade;

    @Column(nullable = false)
    private LocalDate data;

    private String motivo;

    public Movimentacao() {
    }

    public Movimentacao(Produto produto, String tipo, Double quantidade, LocalDate data, String motivo) {
        this(produto, null, tipo, quantidade, data, motivo);
    }

    public Movimentacao(Produto produto, Lote lote, String tipo, Double quantidade, LocalDate data, String motivo) {
        this.produto = produto;
        this.lote = lote;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.data = data;
        this.motivo = motivo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public Lote getLote() { return lote; }
    public void setLote(Lote lote) { this.lote = lote; }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Double getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Double quantidade) {
        this.quantidade = quantidade;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
