package br.edu.hortifruti.model;

public class ResumoDesperdicio {

    private final Produto produto;
    private final Double quantidadeDescartada;

    public ResumoDesperdicio(Produto produto, Double quantidadeDescartada) {
        this.produto = produto;
        this.quantidadeDescartada = quantidadeDescartada;
    }

    public Produto getProduto() { return produto; }
    public Double getQuantidadeDescartada() { return quantidadeDescartada; }
}
