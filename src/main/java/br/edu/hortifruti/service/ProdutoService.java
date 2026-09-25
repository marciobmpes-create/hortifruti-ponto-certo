package br.edu.hortifruti.service;

import java.util.List;
import java.util.Comparator;

import org.springframework.stereotype.Service;

import br.edu.hortifruti.model.Produto;
import br.edu.hortifruti.repository.MovimentacaoRepository;
import br.edu.hortifruti.repository.ProdutoRepository;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final MovimentacaoRepository movimentacaoRepository;

    public ProdutoService(ProdutoRepository produtoRepository,
            MovimentacaoRepository movimentacaoRepository) {
        this.produtoRepository = produtoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    public List<Produto> listarTodos() {
        return produtoRepository.findAll();
    }

    public List<Produto> listarTodos(String ordem) {
        List<Produto> produtos = listarTodos();

        Comparator<Produto> comparador = switch (ordem == null ? "NOME_ASC" : ordem) {
            case "NOME_DESC" -> Comparator.comparing(Produto::getNome, String.CASE_INSENSITIVE_ORDER).reversed();
            case "QUANTIDADE_DESC" -> Comparator.comparing(Produto::getQuantidade).reversed();
            case "QUANTIDADE_ASC" -> Comparator.comparing(Produto::getQuantidade);
            case "CATEGORIA" -> Comparator.comparing(Produto::getCategoria, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Produto::getNome, String.CASE_INSENSITIVE_ORDER);
            case "ESTOQUE_BAIXO" -> Comparator.comparing(this::situacaoEstoque)
                    .thenComparing(Produto::getQuantidade);
            default -> Comparator.comparing(Produto::getNome, String.CASE_INSENSITIVE_ORDER);
        };

        produtos.sort(comparador);
        return produtos;
    }

    private int situacaoEstoque(Produto produto) {
        if (produto.getQuantidade() == 0) return 0;
        if (produto.getQuantidade() <= produto.getEstoqueMinimo()) return 1;
        return 2;
    }

    public Produto salvar(Produto produto) {
        return produtoRepository.save(produto);
    }

    public Produto buscarPorId(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));
    }

    public Produto atualizar(Long id, Produto dadosProduto) {
        Produto produto = buscarPorId(id);
        produto.setNome(dadosProduto.getNome());
        produto.setCategoria(dadosProduto.getCategoria());
        produto.setQuantidade(dadosProduto.getQuantidade());
        produto.setUnidade(dadosProduto.getUnidade());
        produto.setEstoqueMinimo(dadosProduto.getEstoqueMinimo());
        return produtoRepository.save(produto);
    }

    public void excluir(Long id) {
        Produto produto = buscarPorId(id);

        if (movimentacaoRepository.existsByProduto_Id(id)) {
            throw new IllegalArgumentException("Este produto possui movimentações no histórico e não pode ser excluído.");
        }

        produtoRepository.delete(produto);
    }
}
