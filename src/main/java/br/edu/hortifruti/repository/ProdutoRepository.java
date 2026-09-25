package br.edu.hortifruti.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.hortifruti.model.Produto;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findByQuantidadeLessThanEqualOrderByQuantidadeAscNomeAsc(Double quantidade);
}
