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
    return produtoRepository.findAll().stream()
            .filter(Produto::isAtivo)
            .collect(java.util.stream.Collectors.toList());
}

public List<Produto> listarInativos() {
    return produtoRepository.findAll().stream()
            .filter(produto -> !produto.isAtivo())
            .sorted(Comparator.comparing(
                    Produto::getNome,
                    String.CASE_INSENSITIVE_ORDER))
            .collect(java.util.stream.Collectors.toList());
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
    produto.setQuantidade(0.0);
    produto.setAtivo(true);
    return produtoRepository.save(produto);
}

    public Produto buscarPorId(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));
    }

     public boolean possuiHistorico(Long id) {
    return movimentacaoRepository.existsByProduto_Id(id);
}

    public Produto atualizar(Long id, Produto dadosProduto) {
        Produto produto = buscarPorId(id);
        produto.setNome(dadosProduto.getNome());
        produto.setCategoria(dadosProduto.getCategoria());
        produto.setUnidade(dadosProduto.getUnidade());
        produto.setEstoqueMinimo(dadosProduto.getEstoqueMinimo());
        return produtoRepository.save(produto);
    }
    
public void excluir(Long id) {
    Produto produto = buscarPorId(id);

    if (movimentacaoRepository.existsByProduto_Id(id)) {
        throw new IllegalArgumentException(
                "Este produto possui histórico. Utilize a opção de desativar para preservar os registros.");
    }

    if (produto.getQuantidade() != null && produto.getQuantidade() > 0) {
        throw new IllegalArgumentException(
                "Este produto ainda possui estoque. Resolva a quantidade disponível antes de excluí-lo.");
    }

    produtoRepository.delete(produto);
}

public void desativar(Long id) {
    Produto produto = buscarPorId(id);

    if (!produto.isAtivo()) {
        throw new IllegalArgumentException("Este produto já está desativado.");
    }

    produto.setAtivo(false);
    produtoRepository.save(produto);
}

public void reativar(Long id) {
    Produto produto = buscarPorId(id);

    if (produto.isAtivo()) {
        throw new IllegalArgumentException("Este produto já está ativo.");
    }

    produto.setAtivo(true);
    produtoRepository.save(produto);
}

}
