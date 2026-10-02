package br.edu.hortifruti.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
public class BackupUploadExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String arquivoMuitoGrande(Model model) {
        model.addAttribute(
                "erroRestauracao",
                "O arquivo é maior que o limite de upload permitido pelo sistema (padrão de 1 MB). "
                        + "Nenhum dado foi alterado.");
        return "backup/pagina";
    }
}