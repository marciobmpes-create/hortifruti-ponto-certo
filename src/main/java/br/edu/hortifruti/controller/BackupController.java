package br.edu.hortifruti.controller;

import java.io.IOException;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.edu.hortifruti.service.BackupRestauracaoService;
import br.edu.hortifruti.service.BackupRestauracaoService.ResultadoRestauracao;
import br.edu.hortifruti.service.BackupService;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/backup")
public class BackupController {

    private static final Logger LOG = LoggerFactory.getLogger(BackupController.class);

    private final BackupService backupService;
    private final BackupRestauracaoService restauracaoService;

    public BackupController(
            BackupService backupService,
            BackupRestauracaoService restauracaoService) {
        this.backupService = backupService;
        this.restauracaoService = restauracaoService;
    }

    @GetMapping
    public String exibirPagina() {
        return "backup/pagina";
    }

    @PostMapping("/download")
    public String baixar(
            @RequestParam(defaultValue = "") String senha,
            HttpServletResponse response,
            Model model) throws IOException {

        byte[] conteudo;

        try {
            conteudo = backupService.gerarBackup(senha);
        } catch (IllegalArgumentException erro) {
            model.addAttribute("erro", erro.getMessage());
            return "backup/pagina";
        } catch (IllegalStateException erro) {
            LOG.error(
                    "Falha ao gerar o backup: {}",
                    erro.getMessage(),
                    erro);

            model.addAttribute("erro", erro.getMessage());
            return "backup/pagina";
        }

        response.setContentType("application/json; charset=UTF-8");

        response.setHeader(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                        .filename(backupService.gerarNomeArquivo())
                        .build()
                        .toString());

        response.setHeader(
                HttpHeaders.CACHE_CONTROL,
                "no-store");

        response.setContentLength(conteudo.length);

        response.getOutputStream().write(conteudo);
        response.flushBuffer();

        return null;
    }

    @PostMapping("/restore")
    public String restaurar(
            @RequestParam(
                    value = "arquivo",
                    required = false) MultipartFile arquivo,
            @RequestParam(defaultValue = "") String senha,
            RedirectAttributes redirect) {

        if (arquivo == null || arquivo.isEmpty()) {
            redirect.addFlashAttribute(
                    "erroRestauracao",
                    "Selecione um arquivo .json para restaurar.");

            return "redirect:/backup";
        }

        String nomeArquivo = arquivo.getOriginalFilename();

        if (nomeArquivo == null
                || !nomeArquivo
                        .toLowerCase(Locale.ROOT)
                        .endsWith(".json")) {

            redirect.addFlashAttribute(
                    "erroRestauracao",
                    "Arquivo inválido: envie somente um arquivo com a extensão .json.");

            return "redirect:/backup";
        }

        try {
            ResultadoRestauracao resultado =
                    restauracaoService.restaurar(
                            arquivo.getBytes(),
                            senha);

            redirect.addFlashAttribute(
                    "sucessoRestauracao",
                    montarMensagemSucesso(resultado));

        } catch (IllegalArgumentException erro) {
            redirect.addFlashAttribute(
                    "erroRestauracao",
                    erro.getMessage());

        } catch (IOException erro) {
            LOG.error(
                    "Falha ao ler o arquivo enviado para restauração: {}",
                    erro.getClass().getName());

            redirect.addFlashAttribute(
                    "erroRestauracao",
                    "Não foi possível ler o arquivo enviado.");

        } catch (IllegalStateException erro) {
            LOG.error(
                    "Falha ao restaurar o backup: {}",
                    erro.getMessage(),
                    erro);

            redirect.addFlashAttribute(
                    "erroRestauracao",
                    erro.getMessage());

        } catch (RuntimeException erro) {
            LOG.error(
                    "Falha inesperada ao restaurar o backup",
                    erro);

            redirect.addFlashAttribute(
                    "erroRestauracao",
                    "Não foi possível restaurar o backup. Nenhuma alteração foi aplicada.");
        }

        return "redirect:/backup";
    }

    private static String montarMensagemSucesso(
            ResultadoRestauracao r) {

        return "Restauração concluída com sucesso. "
                + "Produtos: "
                + r.produtosCriados()
                + " criados e "
                + r.produtosAtualizados()
                + " atualizados. "
                + "Lotes: "
                + r.lotesCriados()
                + " criados e "
                + r.lotesAtualizados()
                + " atualizados. "
                + "Movimentações: "
                + r.movimentacoesCriadas()
                + " criadas e "
                + r.movimentacoesAtualizadas()
                + " atualizadas.";
    }
}