package com.clinica.sistema.Controlador;

import com.clinica.sistema.Servicio.CitaServicio;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class ReservaControlador {
        private Logger logger = LoggerFactory.getLogger(ReservaControlador.class);

    private final CitaServicio citaServicio = new CitaServicio();

    @GetMapping("/reserva")
    public String mostrarPaginaReservaCita() {
        return "reservarCita";
    }

    @PostMapping("/reserva/confirmar")
public String confirmarCita(@RequestParam("fechaCita") String fecha,
                            @RequestParam("horaCita") String hora,
                            @RequestParam("idMedico") Long idMedico) throws IOException {
    logger.info("Intentando crear cita...");
    citaServicio.crearCita(fecha, hora, idMedico);
    logger.info("Cita creada correctamente.");
    return "redirect:/historial";  // o la ruta correcta
}


}
