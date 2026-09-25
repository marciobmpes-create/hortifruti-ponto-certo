package br.edu.hortifruti.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.hortifruti.model.Lote;

public interface LoteRepository extends JpaRepository<Lote, Long> {

    List<Lote> findByDataValidadeLessThanEqualOrderByDataValidadeAsc(LocalDate dataValidade);
}
