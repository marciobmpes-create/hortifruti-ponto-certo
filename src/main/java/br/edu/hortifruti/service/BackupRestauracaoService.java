package br.edu.hortifruti.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Types;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.hortifruti.model.backup.BackupDados;
import br.edu.hortifruti.model.backup.BackupDados.LoteBackup;
import br.edu.hortifruti.model.backup.BackupDados.MovimentacaoBackup;
import br.edu.hortifruti.model.backup.BackupDados.ProdutoBackup;

@Service
public class BackupRestauracaoService {

    private static final Logger LOG = LoggerFactory.getLogger(BackupRestauracaoService.class);
    private static final String FORMATO = "hortifruti-backup";
    private static final int VERSAO_SUPORTADA = 1;
    private static final int TAMANHO_MAXIMO_TEXTO = 255;

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final String senhaConfigurada;

    public BackupRestauracaoService(
            JdbcTemplate jdbc,
            ObjectMapper objectMapper,
            @Value("${hortifruti.limpeza.senha}") String senhaConfigurada) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.senhaConfigurada = senhaConfigurada;
    }

    public record ResultadoRestauracao(
            int produtosCriados,
            int produtosAtualizados,
            int lotesCriados,
            int lotesAtualizados,
            int movimentacoesCriadas,
            int movimentacoesAtualizadas) {
    }

    private record Contagem(int criados, int atualizados) {
    }

    private enum Banco {
        H2, POSTGRESQL
    }

    @Transactional
    public ResultadoRestauracao restaurar(byte[] conteudo, String senhaInformada) {
        validarSenha(senhaInformada);

        BackupDados dados = lerArquivo(conteudo);
        validar(dados);

        Banco banco = identificarBanco();

        long maiorProdutoAntes = maiorId("produtos");
        long maiorLoteAntes = maiorId("lotes");
        long maiorMovimentacaoAntes = maiorId("movimentacoes");

        Contagem produtos;
        Contagem lotes;
        Contagem movimentacoes;

        try {
            produtos = restaurarProdutos(dados.produtos());
            lotes = restaurarLotes(dados.lotes());
            movimentacoes = restaurarMovimentacoes(dados.movimentacoes());
        } catch (DataAccessException erro) {
            throw new IllegalStateException(
                    "Não foi possível restaurar o backup. Nenhuma alteração foi aplicada.", erro);
        }

        try {
            sincronizarContador(banco, "produtos", maiorProdutoAntes);
            sincronizarContador(banco, "lotes", maiorLoteAntes);
            sincronizarContador(banco, "movimentacoes", maiorMovimentacaoAntes);
        } catch (DataAccessException erro) {
            throw new IllegalStateException(
                    "Falha ao ajustar a numeração automática dos IDs. A restauração foi interrompida.", erro);
        }

        LOG.info(
                "Restauração de backup concluída: produtos {} criados/{} atualizados, "
                        + "lotes {} criados/{} atualizados, movimentações {} criadas/{} atualizadas.",
                produtos.criados(), produtos.atualizados(),
                lotes.criados(), lotes.atualizados(),
                movimentacoes.criados(), movimentacoes.atualizados());

        return new ResultadoRestauracao(
                produtos.criados(), produtos.atualizados(),
                lotes.criados(), lotes.atualizados(),
                movimentacoes.criados(), movimentacoes.atualizados());
    }

    private void validarSenha(String senhaInformada) {
        if (senhaConfigurada == null || senhaConfigurada.isBlank()) {
            throw new IllegalStateException(
                    "A restauração está indisponível: a senha de proteção não está configurada.");
        }

        byte[] esperada = senhaConfigurada.getBytes(StandardCharsets.UTF_8);
        byte[] informada = (senhaInformada == null ? "" : senhaInformada)
                .getBytes(StandardCharsets.UTF_8);

        if (!MessageDigest.isEqual(esperada, informada)) {
            throw new IllegalArgumentException(
                    "Senha incorreta. Nenhum dado foi alterado.");
        }
    }

    private BackupDados lerArquivo(byte[] conteudo) {
        if (conteudo == null || conteudo.length == 0) {
            throw new IllegalArgumentException("O arquivo está vazio.");
        }

        BackupDados dados;

        try {
            dados = objectMapper.readValue(conteudo, BackupDados.class);
        } catch (IOException erro) {
            throw new IllegalArgumentException(
                    "O arquivo não é um JSON válido ou não corresponde ao formato de backup deste sistema.");
        }

        if (dados == null) {
            throw new IllegalArgumentException(
                    "O arquivo não contém dados de backup.");
        }

        return dados;
    }

    private void validar(BackupDados dados) {
        if (!FORMATO.equals(dados.formato())) {
            throw new IllegalArgumentException(
                    "Formato não reconhecido: o arquivo não foi gerado por este sistema.");
        }

        if (dados.versao() != VERSAO_SUPORTADA) {
            throw new IllegalArgumentException(
                    "Versão de backup não suportada: " + dados.versao()
                            + ". Versão aceita: " + VERSAO_SUPORTADA + ".");
        }

        if (dados.produtos() == null
                || dados.lotes() == null
                || dados.movimentacoes() == null) {
            throw new IllegalArgumentException(
                    "Backup incompleto: as listas de produtos, lotes e movimentações são obrigatórias.");
        }

        if (dados.produtos().isEmpty()
                && dados.lotes().isEmpty()
                && dados.movimentacoes().isEmpty()) {
            throw new IllegalArgumentException(
                    "O backup não contém nenhum registro para restaurar.");
        }

        Set<Long> idsProdutos = validarProdutos(dados.produtos());

        Map<Long, Long> produtoDoLote =
                validarLotes(dados.lotes(), idsProdutos);

        validarMovimentacoes(
                dados.movimentacoes(),
                idsProdutos,
                produtoDoLote);
    }

    private Set<Long> validarProdutos(List<ProdutoBackup> produtos) {
        Set<Long> ids = new HashSet<>();

        for (int i = 0; i < produtos.size(); i++) {
            ProdutoBackup produto = produtos.get(i);

            String ref = "Produto na posição " + (i + 1);

            if (produto == null) {
                throw new IllegalArgumentException(ref + " está vazio.");
            }

            exigirId(produto.id(), ref);

            if (!ids.add(produto.id())) {
                throw new IllegalArgumentException(
                        "ID de produto duplicado no backup: " + produto.id() + ".");
            }

            ref = "Produto de ID " + produto.id();

            exigirTexto(produto.nome(), ref, "nome");
            exigirTexto(produto.categoria(), ref, "categoria");
            exigirTexto(produto.unidade(), ref, "unidade");

            exigirNumeroNaoNegativo(
                    produto.quantidade(), ref, "quantidade");

            exigirNumeroNaoNegativo(
                    produto.estoqueMinimo(), ref, "estoqueMinimo");
        }

        return ids;
    }

    private Map<Long, Long> validarLotes(
            List<LoteBackup> lotes,
            Set<Long> idsProdutos) {

        Map<Long, Long> produtoDoLote = new HashMap<>();

        for (int i = 0; i < lotes.size(); i++) {
            LoteBackup lote = lotes.get(i);

            String ref = "Lote na posição " + (i + 1);

            if (lote == null) {
                throw new IllegalArgumentException(ref + " está vazio.");
            }

            exigirId(lote.id(), ref);

            if (produtoDoLote.containsKey(lote.id())) {
                throw new IllegalArgumentException(
                        "ID de lote duplicado no backup: " + lote.id() + ".");
            }

            ref = "Lote de ID " + lote.id();

            if (lote.produtoId() == null) {
                throw new IllegalArgumentException(
                        ref + ": o campo \"produtoId\" é obrigatório.");
            }

            if (!idsProdutos.contains(lote.produtoId())) {
                throw new IllegalArgumentException(
                        ref + " referencia o produto " + lote.produtoId()
                                + ", que não existe no backup.");
            }

            exigirTexto(lote.codigo(), ref, "codigo");
            exigirData(lote.dataEntrada(), ref, "dataEntrada");
            exigirData(lote.dataValidade(), ref, "dataValidade");

            exigirNumeroNaoNegativo(
                    lote.quantidade(), ref, "quantidade");

            produtoDoLote.put(lote.id(), lote.produtoId());
        }

        return produtoDoLote;
    }

    private void validarMovimentacoes(
            List<MovimentacaoBackup> movimentacoes,
            Set<Long> idsProdutos,
            Map<Long, Long> produtoDoLote) {

        Set<Long> ids = new HashSet<>();

        for (int i = 0; i < movimentacoes.size(); i++) {
            MovimentacaoBackup movimentacao = movimentacoes.get(i);

            String ref = "Movimentação na posição " + (i + 1);

            if (movimentacao == null) {
                throw new IllegalArgumentException(ref + " está vazia.");
            }

            exigirId(movimentacao.id(), ref);

            if (!ids.add(movimentacao.id())) {
                throw new IllegalArgumentException(
                        "ID de movimentação duplicado no backup: "
                                + movimentacao.id() + ".");
            }

            ref = "Movimentação de ID " + movimentacao.id();

            if (movimentacao.produtoId() == null) {
                throw new IllegalArgumentException(
                        ref + ": o campo \"produtoId\" é obrigatório.");
            }

            if (!idsProdutos.contains(movimentacao.produtoId())) {
                throw new IllegalArgumentException(
                        ref + " referencia o produto "
                                + movimentacao.produtoId()
                                + ", que não existe no backup.");
            }

            if (movimentacao.loteId() != null) {
                Long produtoDoLoteReferenciado =
                        produtoDoLote.get(movimentacao.loteId());

                if (produtoDoLoteReferenciado == null) {
                    throw new IllegalArgumentException(
                            ref + " referencia o lote "
                                    + movimentacao.loteId()
                                    + ", que não existe no backup.");
                }

                if (!produtoDoLoteReferenciado.equals(
                        movimentacao.produtoId())) {
                    throw new IllegalArgumentException(
                            ref + " referencia um lote que pertence a outro produto.");
                }
            }

            exigirTexto(movimentacao.tipo(), ref, "tipo");

            exigirNumeroNaoNegativo(
                    movimentacao.quantidade(), ref, "quantidade");

            exigirData(movimentacao.data(), ref, "data");

            if (movimentacao.motivo() != null
                    && movimentacao.motivo().length() > TAMANHO_MAXIMO_TEXTO) {
                throw new IllegalArgumentException(
                        ref + ": o campo \"motivo\" excede "
                                + TAMANHO_MAXIMO_TEXTO + " caracteres.");
            }
        }
    }

    private static void exigirId(Long id, String ref) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    ref + ": ID ausente ou inválido.");
        }
    }

    private static void exigirTexto(
            String valor,
            String ref,
            String campo) {

        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    ref + ": o campo \"" + campo + "\" é obrigatório.");
        }

        if (valor.length() > TAMANHO_MAXIMO_TEXTO) {
            throw new IllegalArgumentException(
                    ref + ": o campo \"" + campo + "\" excede "
                            + TAMANHO_MAXIMO_TEXTO + " caracteres.");
        }
    }

    private static void exigirNumeroNaoNegativo(
            Double valor,
            String ref,
            String campo) {

        if (valor == null
                || valor.isNaN()
                || valor.isInfinite()
                || valor < 0) {
            throw new IllegalArgumentException(
                    ref + ": o campo \"" + campo
                            + "\" é obrigatório e não pode ser negativo.");
        }
    }

    private static void exigirData(
            LocalDate valor,
            String ref,
            String campo) {

        if (valor == null) {
            throw new IllegalArgumentException(
                    ref + ": o campo \"" + campo + "\" é obrigatório.");
        }
    }

    private Contagem restaurarProdutos(List<ProdutoBackup> produtos) {
        Set<Long> existentes = idsExistentes("produtos");

        int criados = 0;
        int atualizados = 0;

        for (ProdutoBackup p : produtos) {
            if (existentes.contains(p.id())) {
                jdbc.update(
                        "UPDATE produtos SET nome = ?, categoria = ?, quantidade = ?, unidade = ?, estoque_minimo = ? WHERE id = ?",
                        new Object[] {
                                p.nome(),
                                p.categoria(),
                                p.quantidade(),
                                p.unidade(),
                                p.estoqueMinimo(),
                                p.id()
                        },
                        new int[] {
                                Types.VARCHAR,
                                Types.VARCHAR,
                                Types.DOUBLE,
                                Types.VARCHAR,
                                Types.DOUBLE,
                                Types.BIGINT
                        });

                atualizados++;
            } else {
                jdbc.update(
                        "INSERT INTO produtos (id, nome, categoria, quantidade, unidade, estoque_minimo) VALUES (?, ?, ?, ?, ?, ?)",
                        new Object[] {
                                p.id(),
                                p.nome(),
                                p.categoria(),
                                p.quantidade(),
                                p.unidade(),
                                p.estoqueMinimo()
                        },
                        new int[] {
                                Types.BIGINT,
                                Types.VARCHAR,
                                Types.VARCHAR,
                                Types.DOUBLE,
                                Types.VARCHAR,
                                Types.DOUBLE
                        });

                criados++;
            }
        }

        return new Contagem(criados, atualizados);
    }

    private Contagem restaurarLotes(List<LoteBackup> lotes) {
        Set<Long> existentes = idsExistentes("lotes");

        int criados = 0;
        int atualizados = 0;

        for (LoteBackup l : lotes) {
            if (existentes.contains(l.id())) {
                jdbc.update(
                        "UPDATE lotes SET produto_id = ?, codigo = ?, data_entrada = ?, data_validade = ?, quantidade = ? WHERE id = ?",
                        new Object[] {
                                l.produtoId(),
                                l.codigo(),
                                data(l.dataEntrada()),
                                data(l.dataValidade()),
                                l.quantidade(),
                                l.id()
                        },
                        new int[] {
                                Types.BIGINT,
                                Types.VARCHAR,
                                Types.DATE,
                                Types.DATE,
                                Types.DOUBLE,
                                Types.BIGINT
                        });

                atualizados++;
            } else {
                jdbc.update(
                        "INSERT INTO lotes (id, produto_id, codigo, data_entrada, data_validade, quantidade) VALUES (?, ?, ?, ?, ?, ?)",
                        new Object[] {
                                l.id(),
                                l.produtoId(),
                                l.codigo(),
                                data(l.dataEntrada()),
                                data(l.dataValidade()),
                                l.quantidade()
                        },
                        new int[] {
                                Types.BIGINT,
                                Types.BIGINT,
                                Types.VARCHAR,
                                Types.DATE,
                                Types.DATE,
                                Types.DOUBLE
                        });

                criados++;
            }
        }

        return new Contagem(criados, atualizados);
    }

    private Contagem restaurarMovimentacoes(
            List<MovimentacaoBackup> movimentacoes) {

        Set<Long> existentes = idsExistentes("movimentacoes");

        int criados = 0;
        int atualizados = 0;

        for (MovimentacaoBackup m : movimentacoes) {
            if (existentes.contains(m.id())) {
                jdbc.update(
                        "UPDATE movimentacoes SET produto_id = ?, lote_id = ?, tipo = ?, quantidade = ?, data = ?, motivo = ? WHERE id = ?",
                        new Object[] {
                                m.produtoId(),
                                m.loteId(),
                                m.tipo(),
                                m.quantidade(),
                                data(m.data()),
                                m.motivo(),
                                m.id()
                        },
                        new int[] {
                                Types.BIGINT,
                                Types.BIGINT,
                                Types.VARCHAR,
                                Types.DOUBLE,
                                Types.DATE,
                                Types.VARCHAR,
                                Types.BIGINT
                        });

                atualizados++;
            } else {
                jdbc.update(
                        "INSERT INTO movimentacoes (id, produto_id, lote_id, tipo, quantidade, data, motivo) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        new Object[] {
                                m.id(),
                                m.produtoId(),
                                m.loteId(),
                                m.tipo(),
                                m.quantidade(),
                                data(m.data()),
                                m.motivo()
                        },
                        new int[] {
                                Types.BIGINT,
                                Types.BIGINT,
                                Types.BIGINT,
                                Types.VARCHAR,
                                Types.DOUBLE,
                                Types.DATE,
                                Types.VARCHAR
                        });

                criados++;
            }
        }

        return new Contagem(criados, atualizados);
    }

    private static java.sql.Date data(LocalDate valor) {
        return valor == null ? null : java.sql.Date.valueOf(valor);
    }

    private Set<Long> idsExistentes(String tabela) {
        return new HashSet<>(
                jdbc.queryForList(
                        "SELECT id FROM " + tabela,
                        Long.class));
    }

    private long maiorId(String tabela) {
        Long maior = jdbc.queryForObject(
                "SELECT MAX(id) FROM " + tabela,
                Long.class);

        return maior == null ? 0L : maior;
    }

    private Banco identificarBanco() {
        String nome = jdbc.execute(
                (ConnectionCallback<String>) conexao ->
                        conexao.getMetaData().getDatabaseProductName());

        if ("H2".equalsIgnoreCase(nome)) {
            return Banco.H2;
        }

        if ("PostgreSQL".equalsIgnoreCase(nome)) {
            return Banco.POSTGRESQL;
        }

        throw new IllegalStateException(
                "Banco de dados não suportado pela restauração: "
                        + nome + ".");
    }

    private void sincronizarContador(
            Banco banco,
            String tabela,
            long maiorIdAntes) {

        long maiorIdDepois = maiorId(tabela);

        if (maiorIdDepois <= maiorIdAntes) {
            return;
        }

        if (banco == Banco.POSTGRESQL) {
            String sequencia = jdbc.queryForObject(
                    "SELECT pg_get_serial_sequence(?, 'id')",
                    String.class,
                    tabela);

            if (sequencia == null) {
                throw new IllegalStateException(
                        "Não foi possível localizar a numeração automática da tabela "
                                + tabela + ".");
            }

            Long proximo = jdbc.queryForObject(
                    "SELECT nextval(CAST(? AS regclass))",
                    Long.class,
                    sequencia);

            if (proximo == null || proximo <= maiorIdDepois) {
                jdbc.queryForObject(
                        "SELECT setval(CAST(? AS regclass), ?, true)",
                        Long.class,
                        sequencia,
                        maiorIdDepois);
            }
        } else {
            jdbc.execute(
                    "ALTER TABLE " + tabela
                            + " ALTER COLUMN id RESTART WITH "
                            + (maiorIdDepois + 1));
        }
    }
}