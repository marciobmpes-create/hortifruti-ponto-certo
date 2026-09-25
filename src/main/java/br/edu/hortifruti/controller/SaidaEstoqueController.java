package br.edu.hortifruti.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import br.edu.hortifruti.service.MovimentacaoService;
import br.edu.hortifruti.service.ProdutoService;

@Controller
@RequestMapping("/saidas")
public class SaidaEstoqueController {

    private final ProdutoService produtoService;
    private final MovimentacaoService movimentacaoService;

    public SaidaEstoqueController(ProdutoService produtoService,
            MovimentacaoService movimentacaoService) {
        this.produtoService = produtoService;
        this.movimentacaoService = movimentacaoService;
    }

    @GetMapping("/nova")
    public String exibirFormulario(Model model) {
        model.addAttribute("produtos", produtoService.listarTodos());
        return "saidas/formulario";
    }

    @PostMapping
    public String registrarSaida(@RequestParam(required = false) Long produtoId,
            @RequestParam(required = false) Double quantidade,
            Model model) {
        try {
            movimentacaoService.registrarSaida(produtoId, quantidade);
            return "redirect:/produtos";
        } catch (IllegalArgumentException erro) {
            model.addAttribute("produtos", produtoService.listarTodos());
            model.addAttribute("produtoId", produtoId);
            model.addAttribute("quantidade", quantidade);
            model.addAttribute("erro", erro.getMessage());
            return "saidas/formulario";
        }
    }
}
