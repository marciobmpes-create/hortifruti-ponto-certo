package br.edu.hortifruti.model.backup;

import java.time.LocalDate;
import java.util.List;

/**
 * Estrutura do arquivo de backup (somente exportação).
 * Os vínculos são feitos por identificadores: movimentações e lotes apontam
 * para o produto por {@code produtoId}, e movimentações apontam para o lote
 * (quando houver) por {@code loteId}.
 */
public record BackupDados(
        String formato,
        int versao,
        String geradoEm,
        List<ProdutoBackup> produtos,
        List<LoteBackup> lotes,
        List<MovimentacaoBackup> movimentacoes) {

    public record ProdutoBackup(
            Long id,
            String nome,
            String categoria,
            Double quantidade,
            String unidade,
            Double estoqueMinimo) {
    }

    public record LoteBackup(
            Long id,
            Long produtoId,
            String codigo,
            LocalDate dataEntrada,
            LocalDate dataValidade,
            Double quantidade) {
    }

    public record MovimentacaoBackup(
            Long id,
            Long produtoId,
            Long loteId,
            String tipo,
            Double quantidade,
            LocalDate data,
            String motivo) {
    }
}
