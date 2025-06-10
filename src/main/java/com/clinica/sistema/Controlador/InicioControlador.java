package com.clinica.sistema.Controlador;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InicioControlador {

    private Logger logger = LoggerFactory.getLogger(InicioControlador.class);

    @GetMapping("/")
    public String mostrarPaginaInicio() {
        logger.info("El usuario ha accedido a la página de inicio.");
        return "inicio";
    }
}