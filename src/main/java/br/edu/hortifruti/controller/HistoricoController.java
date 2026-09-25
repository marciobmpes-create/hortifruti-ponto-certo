package br.edu.hortifruti.controller;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.edu.hortifruti.service.MovimentacaoService;
import br.edu.hortifruti.service.ProdutoService;

@Controller
@RequestMapping("/historico")
public class HistoricoController {

    private final MovimentacaoService movimentacaoService;
    private final ProdutoService produtoService;

    public HistoricoController(MovimentacaoService movimentacaoService,
            ProdutoService produtoService) {
        this.movimentacaoService = movimentacaoService;
        this.produtoService = produtoService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) Long produtoId,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            Model model) {
        String tipoSelecionado = tipo == null || tipo.isBlank() ? null : tipo;
        boolean filtrosAplicados = produtoId != null || tipoSelecionado != null
                || dataInicial != null || dataFinal != null;

        model.addAttribute("movimentacoes",
                movimentacaoService.buscarComFiltros(produtoId, tipoSelecionado, dataInicial, dataFinal));
        model.addAttribute("produtos", produtoService.listarTodos());
        model.addAttribute("produtoId", produtoId);
        model.addAttribute("tipo", tipoSelecionado);
        model.addAttribute("dataInicial", dataInicial);
        model.addAttribute("dataFinal", dataFinal);
        model.addAttribute("filtrosAplicados", filtrosAplicados);
        return "historico/lista";
    }

    @GetMapping("/limpar")
    public String exibirConfirmacaoLimpeza() {
        return "historico/confirmar-limpeza";
    }

    @PostMapping("/limpar")
    public String limpar(@RequestParam String senha,
            @RequestParam String confirmacao,
            RedirectAttributes atributosRedirecionamento,
            Model model) {
        try {
            movimentacaoService.limparHistorico(senha, confirmacao);
            atributosRedirecionamento.addFlashAttribute("sucesso",
                    "Histórico limpo com sucesso. O estoque foi zerado e os produtos foram mantidos.");
            return "redirect:/historico";
        } catch (IllegalArgumentException erro) {
            model.addAttribute("erro", erro.getMessage());
            return "historico/confirmar-limpeza";
        }
    }
}
