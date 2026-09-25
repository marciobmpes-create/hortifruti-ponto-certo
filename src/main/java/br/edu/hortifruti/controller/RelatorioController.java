package br.edu.hortifruti.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import br.edu.hortifruti.model.Movimentacao;
import br.edu.hortifruti.service.MovimentacaoService;
import br.edu.hortifruti.service.ProdutoService;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/relatorio")
public class RelatorioController {

    private static final DateTimeFormatter DATA_FORMATADA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MovimentacaoService movimentacaoService;
    private final ProdutoService produtoService;

    public RelatorioController(MovimentacaoService movimentacaoService, ProdutoService produtoService) {
        this.movimentacaoService = movimentacaoService;
        this.produtoService = produtoService;
    }

    @GetMapping
    public String exibirRelatorio(@RequestParam(required = false) Long produtoId,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            Model model) {
        List<Movimentacao> movimentacoes = buscarMovimentacoes(produtoId, tipo, dataInicial, dataFinal);
        preencherModelo(model, movimentacoes, produtoId, tipo, dataInicial, dataFinal);
        return "relatorio/lista";
    }

    @GetMapping("/exportar-csv")
    public void exportarCsv(@RequestParam(required = false) Long produtoId,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=relatorio-movimentacoes.csv");

        try (PrintWriter escritor = response.getWriter()) {
            // BOM: ajuda o Excel a abrir corretamente os caracteres em português.
            escritor.write('\uFEFF');
            escritor.println("Data;Produto;Tipo;Quantidade;Unidade;Motivo");

            for (Movimentacao movimentacao : buscarMovimentacoes(produtoId, tipo, dataInicial, dataFinal)) {
                escritor.printf("%s;%s;%s;%s;%s;%s%n",
                        textoCsv(DATA_FORMATADA.format(movimentacao.getData())),
                        textoCsv(movimentacao.getProduto().getNome()),
                        textoCsv(movimentacao.getTipo()),
                        textoCsv(String.valueOf(movimentacao.getQuantidade())),
                        textoCsv(movimentacao.getProduto().getUnidade()),
                        textoCsv(movimentacao.getMotivo() == null ? "" : movimentacao.getMotivo()));
            }
        }
    }

    private List<Movimentacao> buscarMovimentacoes(Long produtoId, String tipo,
            LocalDate dataInicial, LocalDate dataFinal) {
        String tipoFiltro = tipo == null || tipo.isBlank() ? null : tipo;
        return movimentacaoService.buscarComFiltros(produtoId, tipoFiltro, dataInicial, dataFinal);
    }

    private void preencherModelo(Model model, List<Movimentacao> movimentacoes, Long produtoId,
            String tipo, LocalDate dataInicial, LocalDate dataFinal) {
        model.addAttribute("movimentacoes", movimentacoes);
        model.addAttribute("produtos", produtoService.listarTodos("NOME_ASC"));
        model.addAttribute("produtoId", produtoId);
        model.addAttribute("tipo", tipo == null ? "" : tipo);
        model.addAttribute("dataInicial", dataInicial);
        model.addAttribute("dataFinal", dataFinal);
        model.addAttribute("totalEntradas", totalPorTipo(movimentacoes, "ENTRADA"));
        model.addAttribute("totalSaidas", totalPorTipo(movimentacoes, "SAIDA"));
        model.addAttribute("totalDescartes", totalPorTipo(movimentacoes, "DESCARTE"));
        model.addAttribute("filtrosAplicados", produtoId != null || (tipo != null && !tipo.isBlank())
                || dataInicial != null || dataFinal != null);
    }

    private double totalPorTipo(List<Movimentacao> movimentacoes, String tipo) {
        return movimentacoes.stream()
                .filter(movimentacao -> tipo.equals(movimentacao.getTipo()))
                .mapToDouble(Movimentacao::getQuantidade)
                .sum();
    }

    private String textoCsv(String valor) {
        String texto = valor == null ? "" : valor;
        if (texto.contains(";") || texto.contains("\"") || texto.contains("\n")) {
            return "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }
}
