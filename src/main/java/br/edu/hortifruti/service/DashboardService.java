package br.edu.hortifruti.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import br.edu.hortifruti.model.Movimentacao;
import br.edu.hortifruti.model.Lote;
import br.edu.hortifruti.model.Produto;
import br.edu.hortifruti.model.ResumoDesperdicio;
import br.edu.hortifruti.repository.LoteRepository;
import br.edu.hortifruti.repository.MovimentacaoRepository;
import br.edu.hortifruti.repository.ProdutoRepository;

@Service
public class DashboardService {

    private final ProdutoRepository produtoRepository;
    private final MovimentacaoRepository movimentacaoRepository;
    private final LoteRepository loteRepository;

    public DashboardService(ProdutoRepository produtoRepository,
            MovimentacaoRepository movimentacaoRepository,
            LoteRepository loteRepository) {
        this.produtoRepository = produtoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.loteRepository = loteRepository;
    }

    public long contarProdutos() {
        return produtoRepository.count();
    }

    public double calcularQuantidadeEmEstoque() {
        return produtoRepository.findAll().stream()
                .mapToDouble(produto -> produto.getQuantidade())
                .sum();
    }

    public long contarPorTipo(List<Movimentacao> movimentacoes, String tipo) {
        return movimentacoes.stream().filter(movimentacao -> tipo.equals(movimentacao.getTipo())).count();
    }

    public double calcularQuantidadeDescartada(List<Movimentacao> movimentacoes) {
        return movimentacoes.stream()
                .filter(movimentacao -> "DESCARTE".equals(movimentacao.getTipo()))
                .mapToDouble(Movimentacao::getQuantidade)
                .sum();
    }

    public Double calcularTaxaDesperdicio(List<Movimentacao> movimentacoes) {
        double quantidadeMovimentada = movimentacoes.stream()
                .mapToDouble(Movimentacao::getQuantidade)
                .sum();

        if (quantidadeMovimentada == 0) {
            return null;
        }

        return calcularQuantidadeDescartada(movimentacoes) / quantidadeMovimentada * 100;
    }

    public List<Movimentacao> buscarMovimentacoesDoPeriodo(LocalDate dataInicial, LocalDate dataFinal) {
        return movimentacaoRepository.buscarComFiltros(null, null, dataInicial, dataFinal);
    }

    public List<Produto> listarProdutosComEstoqueBaixo() {
        return produtoRepository.findAll().stream()
                .filter(produto -> produto.getQuantidade() <= produto.getEstoqueMinimo())
                .sorted(Comparator.comparing(Produto::getQuantidade))
                .toList();
    }

    public List<ResumoDesperdicio> resumoDesperdicio(List<Movimentacao> movimentacoes) {
        Map<Produto, Double> quantidades = new LinkedHashMap<>();
        movimentacoes.stream()
                .filter(movimentacao -> "DESCARTE".equals(movimentacao.getTipo()))
                .forEach(movimentacao -> quantidades.merge(movimentacao.getProduto(),
                        movimentacao.getQuantidade(), Double::sum));

        return quantidades.entrySet().stream()
                .map(item -> new ResumoDesperdicio(item.getKey(), item.getValue()))
                .sorted(Comparator.comparing(ResumoDesperdicio::getQuantidadeDescartada).reversed())
                .toList();
    }

    public String motivoMaisFrequente(List<Movimentacao> movimentacoes) {
        Map<String, Long> motivos = new LinkedHashMap<>();
        movimentacoes.stream()
                .filter(movimentacao -> "DESCARTE".equals(movimentacao.getTipo()))
                .filter(movimentacao -> movimentacao.getMotivo() != null && !movimentacao.getMotivo().isBlank())
                .forEach(movimentacao -> motivos.merge(movimentacao.getMotivo(), 1L, Long::sum));

        return motivos.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public List<Lote> listarLotesProximosDaValidade() {
        return loteRepository.findByDataValidadeLessThanEqualOrderByDataValidadeAsc(LocalDate.now().plusDays(7));
    }
}
