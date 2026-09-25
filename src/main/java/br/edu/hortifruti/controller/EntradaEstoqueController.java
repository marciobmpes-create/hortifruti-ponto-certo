package br.edu.hortifruti.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import br.edu.hortifruti.service.MovimentacaoService;
import br.edu.hortifruti.service.ProdutoService;

@Controller
@RequestMapping("/entradas")
public class EntradaEstoqueController {

    private final ProdutoService produtoService;
    private final MovimentacaoService movimentacaoService;

    public EntradaEstoqueController(ProdutoService produtoService,
            MovimentacaoService movimentacaoService) {
        this.produtoService = produtoService;
        this.movimentacaoService = movimentacaoService;
    }

    @GetMapping("/nova")
    public String exibirFormulario(Model model) {
        model.addAttribute("produtos", produtoService.listarTodos());
        return "entradas/formulario";
    }

    @PostMapping
    public String registrarEntrada(@RequestParam(required = false) Long produtoId,
            @RequestParam(required = false) Double quantidade,
            @RequestParam(required = false) String codigoLote,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataValidade,
            Model model) {
        try {
            movimentacaoService.registrarEntrada(produtoId, quantidade, codigoLote, dataValidade);
            return "redirect:/produtos";
        } catch (IllegalArgumentException erro) {
            model.addAttribute("produtos", produtoService.listarTodos());
            model.addAttribute("produtoId", produtoId);
            model.addAttribute("quantidade", quantidade);
            model.addAttribute("codigoLote", codigoLote);
            model.addAttribute("dataValidade", dataValidade);
            model.addAttribute("erro", erro.getMessage());
            return "entradas/formulario";
        }
    }
}
