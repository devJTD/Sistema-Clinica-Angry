package com.clinica.sistema.Controlador;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReservaControlador {

    private Logger logger = LoggerFactory.getLogger(ReservaControlador.class);

    @GetMapping("/reserva")
    public String mostrarPaginaReservaCita() {
        logger.info("El usuario ha accedido a la página de reserva de cita.");
        return "reservarCita"; 
    }

}