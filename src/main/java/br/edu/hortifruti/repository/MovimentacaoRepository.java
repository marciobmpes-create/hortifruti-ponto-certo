package br.edu.hortifruti.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.hortifruti.model.Movimentacao;

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {

    List<Movimentacao> findAllByOrderByDataDescIdDesc();

    long countByTipo(String tipo);

    List<Movimentacao> findByTipo(String tipo);

    boolean existsByProduto_Id(Long produtoId);

    @Query("""
            SELECT m FROM Movimentacao m
            WHERE (:produtoId IS NULL OR m.produto.id = :produtoId)
              AND (:tipo IS NULL OR m.tipo = :tipo)
              AND (:dataInicial IS NULL OR m.data >= :dataInicial)
              AND (:dataFinal IS NULL OR m.data <= :dataFinal)
            ORDER BY m.data DESC, m.id DESC
            """)
    List<Movimentacao> buscarComFiltros(
            @Param("produtoId") Long produtoId,
            @Param("tipo") String tipo,
            @Param("dataInicial") LocalDate dataInicial,
            @Param("dataFinal") LocalDate dataFinal);
}
