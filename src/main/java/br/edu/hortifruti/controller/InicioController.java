package br.edu.hortifruti.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InicioController {

    @GetMapping("/")
    public String redirecionarParaProdutos() {
        return "redirect:/produtos";
    }
}
