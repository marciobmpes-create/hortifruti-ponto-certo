package br.edu.hortifruti.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

/**
 * =====================================================================
 *  ARQUIVO TEMPORÁRIO - CORREÇÃO PONTUAL DE DADOS - USO ÚNICO
 * =====================================================================
 *
 * Criado somente para corrigir a data das movimentações de ID 27 a 37
 * (2026-10-02 -> 2026-10-01) no banco usado pelo aplicativo.
 *
 * COMO REMOVER: apague este arquivo e faça um novo deploy.
 * Nenhum outro arquivo do projeto foi criado ou alterado para esta correção.
 *
 * Segurança:
 *  - IDs e datas estão fixos no código: nada é recebido por parâmetro.
 *  - Exige a senha administrativa já existente (hortifruti.limpeza.senha)
 *    e a palavra CORRIGIR para executar.
 *  - Usa o JdbcTemplate e o gerenciador de transações que o Spring Boot
 *    já configura (nenhum DataSource novo, nenhuma credencial no código).
 *  - Só altera a coluna "data". Se qualquer verificação falhar, a
 *    transação inteira é desfeita.
 */
@RestController
@RequestMapping("/correcao-temporaria-datas")
public class CorrecaoTemporariaDatasController {

    private static final Logger LOG = LoggerFactory.getLogger(CorrecaoTemporariaDatasController.class);

    private static final long ID_INICIAL = 27L;
    private static final long ID_FINAL = 37L;
    private static final int TOTAL_ESPERADO = 11;
    private static final LocalDate DATA_ORIGEM = LocalDate.of(2026, 10, 2);
    private static final LocalDate DATA_DESTINO = LocalDate.of(2026, 10, 1);

    private static final String PALAVRA_CONFIRMACAO = "CORRIGIR";

    private static final String SQL_CONSULTA = "SELECT id, produto_id, tipo, quantidade, data, motivo, lote_id "
            + "FROM movimentacoes WHERE id BETWEEN 27 AND 37 ORDER BY id";

    private static final String SQL_UPDATE = "UPDATE movimentacoes SET data = DATE '2026-10-01' "
            + "WHERE id BETWEEN 27 AND 37 AND data = DATE '2026-10-02'";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacao;
    private final String senhaConfigurada;

    public CorrecaoTemporariaDatasController(JdbcTemplate jdbc,
            PlatformTransactionManager gerenciadorTransacoes,
            @Value("${hortifruti.limpeza.senha}") String senhaConfigurada) {
        this.jdbc = jdbc;
        this.transacao = new TransactionTemplate(gerenciadorTransacoes);
        this.senhaConfigurada = senhaConfigurada;
    }

    private record Linha(long id, long produtoId, String tipo, double quantidade,
            LocalDate data, String motivo, Long loteId) {

        /** Igual à outra linha em todos os campos, exceto a data. */
        boolean igualExcetoData(Linha outra) {
            return id == outra.id
                    && produtoId == outra.produtoId
                    && quantidade == outra.quantidade
                    && Objects.equals(tipo, outra.tipo)
                    && Objects.equals(motivo, outra.motivo)
                    && Objects.equals(loteId, outra.loteId);
        }
    }

    private record Resultado(int linhasAfetadas, List<Linha> antes, List<Linha> depois) {
    }

    // ------------------------------------------------------------------
    // Telas
    // ------------------------------------------------------------------

    @GetMapping(produces = "text/html;charset=UTF-8")
    public String exibirPagina() {
        return pagina("");
    }

    /** Somente leitura: mostra as linhas 27 a 37 e se todas estão em 2026-10-02. */
    @PostMapping(value = "/consultar", produces = "text/html;charset=UTF-8")
    public String consultar(@RequestParam(defaultValue = "") String senha) {
        if (!senhaConfere(senha)) {
            return pagina(erro("Senha incorreta. Nenhum dado foi alterado."));
        }

        try {
            List<Linha> linhas = transacao.execute(status -> buscar(false));
            String situacao;
            try {
                validarEstadoInicial(linhas);
                situacao = ok("Conferido: existem " + TOTAL_ESPERADO + " registros (IDs " + ID_INICIAL + " a "
                        + ID_FINAL + ") e todos estão com data " + DATA_ORIGEM
                        + ". Pronto para a correção.");
            } catch (IllegalStateException divergencia) {
                situacao = erro("Atenção: " + divergencia.getMessage() + " A correção NÃO será permitida.");
            }
            return pagina(situacao + tabela("Registros atuais (IDs 27 a 37)", linhas));
        } catch (RuntimeException falha) {
            LOG.error("Falha ao consultar movimentações na correção temporária", falha);
            return pagina(erro("Não foi possível consultar o banco. Nenhum dado foi alterado."));
        }
    }

    /** Altera somente a coluna data, dentro de uma transação com verificações. */
    @PostMapping(value = "/executar", produces = "text/html;charset=UTF-8")
    public String executar(@RequestParam(defaultValue = "") String senha,
            @RequestParam(defaultValue = "") String confirmacao) {
        if (!senhaConfere(senha)) {
            return pagina(erro("Senha incorreta. Nenhum dado foi alterado."));
        }

        if (!PALAVRA_CONFIRMACAO.equalsIgnoreCase(confirmacao.trim())) {
            return pagina(erro("Digite " + PALAVRA_CONFIRMACAO + " para executar. Nenhum dado foi alterado."));
        }

        try {
            Resultado resultado = transacao.execute(status -> corrigir());

            String resumo = ok("UPDATE " + resultado.linhasAfetadas() + " — correção concluída. "
                    + "Verificado: " + resultado.depois().size() + " registros, todos com data " + DATA_DESTINO
                    + "; produto_id, tipo, quantidade, motivo e lote_id inalterados.");
            return pagina(resumo
                    + tabela("Depois da correção (IDs 27 a 37)", resultado.depois())
                    + tabela("Antes da correção (para comparação)", resultado.antes()));
        } catch (IllegalStateException recusa) {
            // Mensagens próprias desta classe: seguras para exibir. A transação foi desfeita.
            LOG.warn("Correção temporária recusada/desfeita: {}", recusa.getMessage());
            return pagina(erro(recusa.getMessage() + " Nenhum dado foi alterado."));
        } catch (RuntimeException falha) {
            LOG.error("Falha inesperada na correção temporária das datas", falha);
            return pagina(erro("Falha inesperada. A transação foi desfeita e nenhum dado foi alterado."));
        }
    }

    // ------------------------------------------------------------------
    // Lógica (executada dentro da transação)
    // ------------------------------------------------------------------

    private Resultado corrigir() {
        // FOR UPDATE trava as linhas até o fim da transação.
        List<Linha> antes = buscar(true);
        validarEstadoInicial(antes);

        int afetadas = jdbc.update(SQL_UPDATE);
        if (afetadas != TOTAL_ESPERADO) {
            throw new IllegalStateException(
                    "O UPDATE afetaria " + afetadas + " linhas em vez de " + TOTAL_ESPERADO + ". Operação desfeita.");
        }

        List<Linha> depois = buscar(false);
        validarEstadoFinal(antes, depois);

        return new Resultado(afetadas, antes, depois);
    }

    private List<Linha> buscar(boolean travar) {
        String sql = travar ? SQL_CONSULTA + " FOR UPDATE" : SQL_CONSULTA;
        return jdbc.query(sql, (rs, numero) -> mapear(rs));
    }

    private static Linha mapear(ResultSet rs) throws SQLException {
        long loteId = rs.getLong("lote_id");
        boolean semLote = rs.wasNull();

        return new Linha(
                rs.getLong("id"),
                rs.getLong("produto_id"),
                rs.getString("tipo"),
                rs.getDouble("quantidade"),
                rs.getObject("data", LocalDate.class),
                rs.getString("motivo"),
                semLote ? null : loteId);
    }

    private static void validarEstadoInicial(List<Linha> linhas) {
        if (linhas.size() != TOTAL_ESPERADO) {
            throw new IllegalStateException(
                    "Foram encontrados " + linhas.size() + " registros entre os IDs " + ID_INICIAL + " e "
                            + ID_FINAL + "; o esperado era " + TOTAL_ESPERADO + ".");
        }

        Set<Long> ids = new HashSet<>();
        for (Linha linha : linhas) {
            ids.add(linha.id());
            if (!DATA_ORIGEM.equals(linha.data())) {
                throw new IllegalStateException(
                        "O registro de ID " + linha.id() + " não está com data " + DATA_ORIGEM
                                + " (data atual: " + linha.data() + ").");
            }
        }

        for (long id = ID_INICIAL; id <= ID_FINAL; id++) {
            if (!ids.contains(id)) {
                throw new IllegalStateException("O registro de ID " + id + " não existe.");
            }
        }
    }

    private static void validarEstadoFinal(List<Linha> antes, List<Linha> depois) {
        if (depois.size() != TOTAL_ESPERADO || antes.size() != TOTAL_ESPERADO) {
            throw new IllegalStateException(
                    "Após o UPDATE, a conferência encontrou " + depois.size() + " registros em vez de "
                            + TOTAL_ESPERADO + ". Operação desfeita.");
        }

        for (int i = 0; i < TOTAL_ESPERADO; i++) {
            Linha antiga = antes.get(i);
            Linha nova = depois.get(i);

            if (!DATA_DESTINO.equals(nova.data())) {
                throw new IllegalStateException(
                        "Após o UPDATE, o registro de ID " + nova.id() + " ficou com data " + nova.data()
                                + ". Operação desfeita.");
            }
            if (!antiga.igualExcetoData(nova)) {
                throw new IllegalStateException(
                        "Após o UPDATE, o registro de ID " + nova.id()
                                + " apresentou diferença em outro campo além da data. Operação desfeita.");
            }
        }
    }

    // ------------------------------------------------------------------
    // Senha
    // ------------------------------------------------------------------

    private boolean senhaConfere(String senhaInformada) {
        if (senhaConfigurada == null || senhaConfigurada.isBlank()) {
            return false;
        }
        byte[] esperada = senhaConfigurada.getBytes(StandardCharsets.UTF_8);
        byte[] informada = (senhaInformada == null ? "" : senhaInformada).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(esperada, informada);
    }

    // ------------------------------------------------------------------
    // HTML mínimo (sem template, para não criar nenhum outro arquivo)
    // ------------------------------------------------------------------

    private static String pagina(String conteudo) {
        return "<!DOCTYPE html><html lang=\"pt-BR\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "<title>Correção temporária de datas</title>"
                + "<style>body{font-family:sans-serif;max-width:900px;margin:2rem auto;padding:0 1rem}"
                + "table{border-collapse:collapse;width:100%;margin:1rem 0}"
                + "th,td{border:1px solid #bbb;padding:.35rem .6rem;text-align:left}"
                + "th{background:#eee}.erro{color:#b71c1c;font-weight:bold}.ok{color:#1b5e20;font-weight:bold}"
                + ".aviso{background:#fff8e1;border-left:4px solid #f9a825;padding:.6rem 1rem}"
                + "label{display:block;margin-top:.8rem}input{padding:.4rem;width:16rem}"
                + "button{margin-top:1rem;padding:.5rem 1rem;margin-right:.5rem}</style></head><body>"
                + "<h1>Correção temporária de datas</h1>"
                + "<p class=\"aviso\">Ferramenta de uso único. Altera somente a coluna <code>data</code> das "
                + "movimentações de ID 27 a 37 (2026-10-02 &rarr; 2026-10-01). Remova este recurso depois do uso.</p>"
                + conteudo
                + "<form method=\"post\">"
                + "<label>Senha administrativa<br><input name=\"senha\" type=\"password\" required "
                + "autocomplete=\"off\"></label>"
                + "<label>Para executar, digite " + PALAVRA_CONFIRMACAO + "<br>"
                + "<input name=\"confirmacao\" type=\"text\" autocomplete=\"off\"></label><br>"
                + "<button type=\"submit\" formaction=\"/correcao-temporaria-datas/consultar\">"
                + "Consultar (somente leitura)</button>"
                + "<button type=\"submit\" formaction=\"/correcao-temporaria-datas/executar\" "
                + "onclick=\"return confirm('Alterar a data dos 11 registros (IDs 27 a 37) para 2026-10-01?');\">"
                + "Executar correção</button>"
                + "</form></body></html>";
    }

    private static String erro(String mensagem) {
        return "<p class=\"erro\">" + HtmlUtils.htmlEscape(mensagem) + "</p>";
    }

    private static String ok(String mensagem) {
        return "<p class=\"ok\">" + HtmlUtils.htmlEscape(mensagem) + "</p>";
    }

    private static String tabela(String titulo, List<Linha> linhas) {
        StringBuilder html = new StringBuilder();
        html.append("<h2>").append(HtmlUtils.htmlEscape(titulo)).append("</h2>");
        html.append("<table><tr><th>id</th><th>produto_id</th><th>tipo</th><th>quantidade</th>"
                + "<th>data</th><th>motivo</th><th>lote_id</th></tr>");

        for (Linha linha : linhas) {
            html.append("<tr><td>").append(linha.id())
                    .append("</td><td>").append(linha.produtoId())
                    .append("</td><td>").append(HtmlUtils.htmlEscape(String.valueOf(linha.tipo())))
                    .append("</td><td>").append(linha.quantidade())
                    .append("</td><td>").append(linha.data())
                    .append("</td><td>").append(linha.motivo() == null ? "" : HtmlUtils.htmlEscape(linha.motivo()))
                    .append("</td><td>").append(linha.loteId() == null ? "" : linha.loteId())
                    .append("</td></tr>");
        }

        html.append("</table>");
        return html.toString();
    }
}
