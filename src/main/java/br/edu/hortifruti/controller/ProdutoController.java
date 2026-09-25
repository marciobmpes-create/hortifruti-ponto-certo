package br.edu.hortifruti.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.edu.hortifruti.model.Produto;
import br.edu.hortifruti.service.ProdutoService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "NOME_ASC") String ordem, Model model) {
        model.addAttribute("produtos", produtoService.listarTodos(ordem));
        model.addAttribute("ordem", ordem);
        return "produtos/lista";
    }

    @GetMapping("/novo")
    public String exibirFormulario(Model model) {
        model.addAttribute("produto", new Produto());
        return "produtos/formulario";
    }

    @PostMapping
    public String salvar(@Valid @ModelAttribute("produto") Produto produto, BindingResult resultado,
            RedirectAttributes atributosRedirecionamento) {
        if (resultado.hasErrors()) {
            return "produtos/formulario";
        }

        produtoService.salvar(produto);
        atributosRedirecionamento.addFlashAttribute("sucesso", "Produto cadastrado com sucesso.");
        return "redirect:/produtos";
    }

    @GetMapping("/{id}/editar")
    public String exibirFormularioEdicao(@PathVariable Long id, Model model) {
        model.addAttribute("produto", produtoService.buscarPorId(id));
        return "produtos/formulario";
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("produto") Produto produto,
            BindingResult resultado, RedirectAttributes atributosRedirecionamento) {
        produto.setId(id);

        if (resultado.hasErrors()) {
            return "produtos/formulario";
        }

        produtoService.atualizar(id, produto);
        atributosRedirecionamento.addFlashAttribute("sucesso", "Produto atualizado com sucesso.");
        return "redirect:/produtos";
    }

    @GetMapping("/{id}/excluir")
    public String exibirConfirmacaoExclusao(@PathVariable Long id, Model model) {
        model.addAttribute("produto", produtoService.buscarPorId(id));
        return "produtos/confirmar-exclusao";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes atributosRedirecionamento) {
        try {
            produtoService.excluir(id);
            atributosRedirecionamento.addFlashAttribute("sucesso", "Produto excluído com sucesso.");
        } catch (IllegalArgumentException erro) {
            atributosRedirecionamento.addFlashAttribute("erro", erro.getMessage());
        }

        return "redirect:/produtos";
    }
}
