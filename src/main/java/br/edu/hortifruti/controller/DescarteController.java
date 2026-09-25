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
@RequestMapping("/descartes")
public class DescarteController {

    private final ProdutoService produtoService;
    private final MovimentacaoService movimentacaoService;

    public DescarteController(ProdutoService produtoService,
            MovimentacaoService movimentacaoService) {
        this.produtoService = produtoService;
        this.movimentacaoService = movimentacaoService;
    }

    @GetMapping("/novo")
    public String exibirFormulario(Model model) {
        model.addAttribute("produtos", produtoService.listarTodos());
        return "descartes/formulario";
    }

    @PostMapping
    public String registrarDescarte(@RequestParam(required = false) Long produtoId,
            @RequestParam(required = false) Double quantidade,
            @RequestParam(required = false) String motivo,
            Model model) {
        try {
            movimentacaoService.registrarDescarte(produtoId, quantidade, motivo);
            return "redirect:/produtos";
        } catch (IllegalArgumentException erro) {
            model.addAttribute("produtos", produtoService.listarTodos());
            model.addAttribute("produtoId", produtoId);
            model.addAttribute("quantidade", quantidade);
            model.addAttribute("motivo", motivo);
            model.addAttribute("erro", erro.getMessage());
            return "descartes/formulario";
        }
    }
}
