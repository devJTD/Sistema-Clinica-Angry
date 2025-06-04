package com.clinica.sistema.Controlador;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HistorialControlador {

    private Logger logger = LoggerFactory.getLogger(HistorialControlador.class);

    @GetMapping("/historial")
    public String mostrarPaginaHistorialCitas() {
        logger.info("El usuario ha accedido a la página de historial de citas.");
        return "historialCita"; 
    }
}