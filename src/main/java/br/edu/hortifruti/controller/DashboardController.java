package br.edu.hortifruti.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import br.edu.hortifruti.model.Movimentacao;
import br.edu.hortifruti.service.DashboardService;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public String exibirDashboard(@RequestParam(defaultValue = "MES_ATUAL") String periodo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            Model model) {
        LocalDate hoje = LocalDate.now();
        LocalDate inicio = dataInicial;
        LocalDate fim = dataFinal;

        switch (periodo) {
            case "HOJE" -> { inicio = hoje; fim = hoje; }
            case "ULTIMOS_7_DIAS" -> { inicio = hoje.minusDays(6); fim = hoje; }
            case "MES_ATUAL" -> { inicio = YearMonth.now().atDay(1); fim = hoje; }
            default -> periodo = "PERSONALIZADO";
        }

        List<Movimentacao> movimentacoes = dashboardService.buscarMovimentacoesDoPeriodo(inicio, fim);
        model.addAttribute("totalProdutos", dashboardService.contarProdutos());
        model.addAttribute("quantidadeEmEstoque", dashboardService.calcularQuantidadeEmEstoque());
        model.addAttribute("totalEntradas", dashboardService.contarPorTipo(movimentacoes, "ENTRADA"));
        model.addAttribute("totalSaidas", dashboardService.contarPorTipo(movimentacoes, "SAIDA"));
        model.addAttribute("totalDescartes", dashboardService.contarPorTipo(movimentacoes, "DESCARTE"));
        model.addAttribute("quantidadeDescartada", dashboardService.calcularQuantidadeDescartada(movimentacoes));
        model.addAttribute("taxaDesperdicio", dashboardService.calcularTaxaDesperdicio(movimentacoes));
        model.addAttribute("resumoDesperdicio", dashboardService.resumoDesperdicio(movimentacoes));
        model.addAttribute("motivoMaisFrequente", dashboardService.motivoMaisFrequente(movimentacoes));
        model.addAttribute("produtosEstoqueBaixo", dashboardService.listarProdutosComEstoqueBaixo());
        model.addAttribute("lotesProximos", dashboardService.listarLotesProximosDaValidade());
        model.addAttribute("hoje", hoje);
        model.addAttribute("periodo", periodo);
        model.addAttribute("dataInicial", inicio);
        model.addAttribute("dataFinal", fim);
        return "dashboard";
    }
}
