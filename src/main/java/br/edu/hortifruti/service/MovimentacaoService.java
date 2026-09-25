package br.edu.hortifruti.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.hortifruti.model.Movimentacao;
import br.edu.hortifruti.model.Lote;
import br.edu.hortifruti.model.Produto;
import br.edu.hortifruti.repository.LoteRepository;
import br.edu.hortifruti.repository.MovimentacaoRepository;
import br.edu.hortifruti.repository.ProdutoRepository;

@Service
public class MovimentacaoService {

    private final ProdutoRepository produtoRepository;
    private final MovimentacaoRepository movimentacaoRepository;
    private final LoteRepository loteRepository;
    private final String senhaLimpeza;

    public MovimentacaoService(ProdutoRepository produtoRepository,
            MovimentacaoRepository movimentacaoRepository,
            LoteRepository loteRepository,
            @Value("${hortifruti.limpeza.senha}") String senhaLimpeza) {
        this.produtoRepository = produtoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.loteRepository = loteRepository;
        this.senhaLimpeza = senhaLimpeza;
    }

    public List<Movimentacao> listarTodas() {
        return movimentacaoRepository.findAllByOrderByDataDescIdDesc();
    }

    public List<Movimentacao> buscarComFiltros(Long produtoId, String tipo,
            LocalDate dataInicial, LocalDate dataFinal) {
        return movimentacaoRepository.buscarComFiltros(produtoId, tipo, dataInicial, dataFinal);
    }

    @Transactional
    public void registrarEntrada(Long produtoId, Double quantidade) {
        registrarEntrada(produtoId, quantidade, null, null);
    }

    @Transactional
    public void registrarEntrada(Long produtoId, Double quantidade, String codigoLote, LocalDate dataValidade) {
        if (produtoId == null) {
            throw new IllegalArgumentException("Selecione um produto.");
        }

        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade da entrada deve ser maior que zero.");
        }

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));

        produto.setQuantidade(produto.getQuantidade() + quantidade);
        produtoRepository.save(produto);

        Lote lote = null;
        boolean informouLote = codigoLote != null && !codigoLote.isBlank();
        if (informouLote != (dataValidade != null)) {
            throw new IllegalArgumentException("Informe o código e a validade do lote, ou deixe ambos vazios.");
        }

        if (informouLote) {
            lote = loteRepository.save(new Lote(produto, codigoLote.trim(), LocalDate.now(), dataValidade, quantidade));
        }

        Movimentacao movimentacao = new Movimentacao(
                produto,
                lote,
                "ENTRADA",
                quantidade,
                LocalDate.now(),
                null);
        movimentacaoRepository.save(movimentacao);
    }

    @Transactional
    public void registrarSaida(Long produtoId, Double quantidade) {
        if (produtoId == null) {
            throw new IllegalArgumentException("Selecione um produto.");
        }

        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade da saída deve ser maior que zero.");
        }

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));

        if (quantidade > produto.getQuantidade()) {
            throw new IllegalArgumentException("A quantidade da saída não pode ser maior que o estoque atual.");
        }

        produto.setQuantidade(produto.getQuantidade() - quantidade);
        produtoRepository.save(produto);

        Movimentacao movimentacao = new Movimentacao(
                produto,
                "SAIDA",
                quantidade,
                LocalDate.now(),
                null);
        movimentacaoRepository.save(movimentacao);
    }

    @Transactional
    public void registrarDescarte(Long produtoId, Double quantidade, String motivo) {
        if (produtoId == null) {
            throw new IllegalArgumentException("Selecione um produto.");
        }

        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade do descarte deve ser maior que zero.");
        }

        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Selecione um motivo para o descarte.");
        }

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));

        if (quantidade > produto.getQuantidade()) {
            throw new IllegalArgumentException("A quantidade do descarte não pode ser maior que o estoque atual.");
        }

        produto.setQuantidade(produto.getQuantidade() - quantidade);
        produtoRepository.save(produto);

        Movimentacao movimentacao = new Movimentacao(
                produto,
                "DESCARTE",
                quantidade,
                LocalDate.now(),
                motivo);
        movimentacaoRepository.save(movimentacao);
    }

    @Transactional
    public void limparHistorico(String senha, String confirmacao) {
        if (!senhaLimpeza.equals(senha)) {
            throw new IllegalArgumentException("Senha incorreta. Nenhum dado foi alterado.");
        }

        if (!"CONFIRMAR".equalsIgnoreCase(confirmacao == null ? "" : confirmacao.trim())) {
            throw new IllegalArgumentException("Digite CONFIRMAR para concluir a limpeza.");
        }

        // A ordem evita referências a lotes que ainda estejam em movimentações.
        movimentacaoRepository.deleteAllInBatch();
        loteRepository.deleteAllInBatch();

        List<Produto> produtos = produtoRepository.findAll();
        produtos.forEach(produto -> produto.setQuantidade(0.0));
        produtoRepository.saveAll(produtos);
    }
}
