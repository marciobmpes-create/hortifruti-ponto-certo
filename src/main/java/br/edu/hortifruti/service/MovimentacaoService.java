package br.edu.hortifruti.service;

import java.time.LocalDate;
import java.time.ZoneId;
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
    private static final ZoneId FUSO_HORARIO = ZoneId.of("America/Sao_Paulo");

public LocalDate hoje() {
    return LocalDate.now(FUSO_HORARIO);
}

@Transactional(readOnly = true)
public Movimentacao buscarPorId(Long id) {
    return movimentacaoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Movimentação não encontrada."));
}

@Transactional
public void atualizarData(Long id, LocalDate novaData, String senha) {
    if (!senhaLimpeza.equals(senha)) {
        throw new IllegalArgumentException("Senha incorreta. Nenhum dado foi alterado.");
    }

    if (id == null) {
        throw new IllegalArgumentException("Movimentação não informada.");
    }

    if (novaData == null) {
        throw new IllegalArgumentException("Informe a nova data.");
    }

    if (novaData.isAfter(hoje())) {
        throw new IllegalArgumentException("A nova data não pode ser uma data futura.");
    }

    if (!movimentacaoRepository.existsById(id)) {
        throw new IllegalArgumentException("Movimentação não encontrada.");
    }

   Movimentacao movimentacao = buscarPorId(id);

List<Movimentacao> movimentacoesDoProduto =
        movimentacaoRepository.findByProduto_IdOrderByDataAscIdAsc(
                movimentacao.getProduto().getId());

List<Movimentacao> movimentacoesOrdenadas = movimentacoesDoProduto.stream()
        .sorted((a, b) -> {
            LocalDate dataA = a.getId().equals(id) ? novaData : a.getData();
            LocalDate dataB = b.getId().equals(id) ? novaData : b.getData();

            int comparacaoData = dataA.compareTo(dataB);

            if (comparacaoData != 0) {
                return comparacaoData;
            }

            return a.getId().compareTo(b.getId());
        })
        .toList();

double estoqueCronologico = 0.0;

for (Movimentacao outra : movimentacoesOrdenadas) {

    if ("ENTRADA".equals(outra.getTipo())) {
        estoqueCronologico += outra.getQuantidade();

    } else if ("SAIDA".equals(outra.getTipo())
            || "DESCARTE".equals(outra.getTipo())) {

        estoqueCronologico -= outra.getQuantidade();

        if (estoqueCronologico < 0) {
            throw new IllegalArgumentException(
                    "A nova data deixaria o histórico do produto com estoque negativo.");
        }
    }
}

    int linhasAfetadas = movimentacaoRepository.atualizarSomenteData(id, novaData);

    if (linhasAfetadas != 1) {
        throw new IllegalStateException(
                "A atualização afetaria " + linhasAfetadas
                        + " registros em vez de 1. Operação desfeita.");
    }
}

@Transactional
public void apagarMovimentacao(Long id, String senha) {
    if (!senhaLimpeza.equals(senha)) {
        throw new IllegalArgumentException("Senha incorreta. Nenhum dado foi alterado.");
    }

    if (id == null) {
        throw new IllegalArgumentException("Movimentação não informada.");
    }

    Movimentacao movimentacao = movimentacaoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Movimentação não encontrada."));

    Produto produto = movimentacao.getProduto();

    List<Movimentacao> movimentacoesDoProduto =
            movimentacaoRepository.findByProduto_IdOrderByDataAscIdAsc(produto.getId());

    List<Movimentacao> movimentacoesSemAExcluida = movimentacoesDoProduto.stream()
            .filter(outra -> !outra.getId().equals(id))
            .toList();

    double estoqueCronologico = 0.0;

    for (Movimentacao outra : movimentacoesSemAExcluida) {

        if ("ENTRADA".equals(outra.getTipo())) {
            estoqueCronologico += outra.getQuantidade();

        } else if ("SAIDA".equals(outra.getTipo())
                || "DESCARTE".equals(outra.getTipo())) {

            estoqueCronologico -= outra.getQuantidade();

            if (estoqueCronologico < 0) {
                throw new IllegalArgumentException(
                        "Não é possível apagar esta movimentação porque o histórico do produto ficaria com estoque negativo.");
            }
        }
    }

    double quantidade = movimentacao.getQuantidade();

    if ("ENTRADA".equals(movimentacao.getTipo())) {
        if (quantidade > produto.getQuantidade()) {
            throw new IllegalArgumentException(
                    "Não é possível apagar esta entrada porque a quantidade já foi utilizada pelo estoque.");
        }

        produto.setQuantidade(produto.getQuantidade() - quantidade);

    } else if ("SAIDA".equals(movimentacao.getTipo())
            || "DESCARTE".equals(movimentacao.getTipo())) {

        produto.setQuantidade(produto.getQuantidade() + quantidade);

    } else {
        throw new IllegalArgumentException("Tipo de movimentação inválido.");
    }

    produtoRepository.save(produto);

    int linhasAfetadas = movimentacaoRepository.apagarPorId(id);

    if (linhasAfetadas != 1) {
        throw new IllegalStateException(
                "A exclusão afetaria " + linhasAfetadas
                        + " registros em vez de 1. Operação desfeita.");
    }
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

        if (dataValidade != null && dataValidade.isBefore(hoje())) {
         throw new IllegalArgumentException("A data de validade não pode ser anterior à data atual.");
        }

        if (informouLote) {
            lote = loteRepository.save(new Lote(produto, codigoLote.trim(), LocalDate.now(), dataValidade, quantidade));
        }

        Movimentacao movimentacao = new Movimentacao(
                produto,
                lote,
                "ENTRADA",
                quantidade,
                LocalDate.now(ZoneId.of("America/Sao_Paulo")),
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
                LocalDate.now(ZoneId.of("America/Sao_Paulo")),
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
                LocalDate.now(ZoneId.of("America/Sao_Paulo")),
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
