package com.clinica.sistema.Controlador;

import com.clinica.sistema.Modelo.Cita;
import com.clinica.sistema.Servicio.CitaServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class HistorialControlador {

    private Logger logger = LoggerFactory.getLogger(HistorialControlador.class);
    private CitaServicio citaServicio = new CitaServicio();

    @GetMapping("/historial")
    public String mostrarPaginaHistorialCitas(Model model) {
        logger.info("El usuario ha accedido a la página de historial de citas.");

        try {
            // Obtener todas las citas
            List<Cita> todasCitas = citaServicio.leerCitas();

            // Filtrar solo las pendientes
            List<Cita> citasPendientes = todasCitas.stream()
                .filter(cita -> "Pendiente".equalsIgnoreCase(cita.getEstado()))
                .collect(Collectors.toList());

            model.addAttribute("citasPendientes", citasPendientes);

            // Nombre de usuario fijo por ahora
            model.addAttribute("nombreUsuario", "Jeanpierre Chipa");

        } catch (IOException e) {
            logger.error("Error al leer citas", e);
            model.addAttribute("citasPendientes", List.of());
        }

        return "historialCita";
    }
}
