package br.edu.hortifruti.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.hortifruti.model.Lote;
import br.edu.hortifruti.model.Movimentacao;
import br.edu.hortifruti.model.Produto;
import br.edu.hortifruti.model.backup.BackupDados;
import br.edu.hortifruti.model.backup.BackupDados.LoteBackup;
import br.edu.hortifruti.model.backup.BackupDados.MovimentacaoBackup;
import br.edu.hortifruti.model.backup.BackupDados.ProdutoBackup;
import br.edu.hortifruti.repository.LoteRepository;

/**
 * Gera o backup em JSON. Somente leitura: nenhum dado é alterado ou apagado.
 */
@Service
public class BackupService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATO_NOME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final ProdutoService produtoService;
    private final MovimentacaoService movimentacaoService;
    private final LoteRepository loteRepository;
    private final ObjectMapper objectMapper;
    private final String senhaConfigurada;

    public BackupService(ProdutoService produtoService,
            MovimentacaoService movimentacaoService,
            LoteRepository loteRepository,
            ObjectMapper objectMapper,
            @Value("${hortifruti.limpeza.senha}") String senhaConfigurada) {
        this.produtoService = produtoService;
        this.movimentacaoService = movimentacaoService;
        this.loteRepository = loteRepository;
        this.objectMapper = objectMapper;
        this.senhaConfigurada = senhaConfigurada;
    }

    public String gerarNomeArquivo() {
        return "backup-hortifruti-" + ZonedDateTime.now(FUSO).format(FORMATO_NOME) + ".json";
    }

    /**
     * Valida a senha e, somente se estiver correta, gera o JSON do backup.
     *
     * @throws IllegalArgumentException senha incorreta (nada é gerado)
     * @throws IllegalStateException    falha ao gerar o backup
     */
    @Transactional(readOnly = true)
    public byte[] gerarBackup(String senhaInformada) {
        validarSenha(senhaInformada);

        try {
            BackupDados dados = montarDados();
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(dados);
        } catch (Exception erro) {
            throw new IllegalStateException("Não foi possível gerar o backup. Tente novamente.", erro);
        }
    }

    private void validarSenha(String senhaInformada) {
        if (senhaConfigurada == null || senhaConfigurada.isBlank()) {
            throw new IllegalStateException("O backup está indisponível: a senha de proteção não está configurada.");
        }

        byte[] esperada = senhaConfigurada.getBytes(StandardCharsets.UTF_8);
        byte[] informada = (senhaInformada == null ? "" : senhaInformada).getBytes(StandardCharsets.UTF_8);

        if (!MessageDigest.isEqual(esperada, informada)) {
            throw new IllegalArgumentException("Senha incorreta. O backup não foi gerado.");
        }
    }

    private BackupDados montarDados() {
        List<ProdutoBackup> produtos = produtoService.listarTodos().stream()
                .sorted(Comparator.comparing(Produto::getId))
                .map(produto -> new ProdutoBackup(
                        produto.getId(),
                        produto.getNome(),
                        produto.getCategoria(),
                        produto.getQuantidade(),
                        produto.getUnidade(),
                        produto.getEstoqueMinimo()))
                .toList();

        List<LoteBackup> lotes = loteRepository.findAll().stream()
                .sorted(Comparator.comparing(Lote::getId))
                .map(lote -> new LoteBackup(
                        lote.getId(),
                        lote.getProduto() == null ? null : lote.getProduto().getId(),
                        lote.getCodigo(),
                        lote.getDataEntrada(),
                        lote.getDataValidade(),
                        lote.getQuantidade()))
                .toList();

        List<MovimentacaoBackup> movimentacoes = movimentacaoService.listarTodas().stream()
                .sorted(Comparator.comparing(Movimentacao::getId))
                .map(movimentacao -> new MovimentacaoBackup(
                        movimentacao.getId(),
                        movimentacao.getProduto() == null ? null : movimentacao.getProduto().getId(),
                        movimentacao.getLote() == null ? null : movimentacao.getLote().getId(),
                        movimentacao.getTipo(),
                        movimentacao.getQuantidade(),
                        movimentacao.getData(),
                        movimentacao.getMotivo()))
                .toList();

        return new BackupDados(
                "hortifruti-backup",
                1,
                ZonedDateTime.now(FUSO).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                produtos,
                lotes,
                movimentacoes);
    }
}
