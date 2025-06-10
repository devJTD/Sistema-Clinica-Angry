package com.clinica.sistema.Controlador;

import com.clinica.sistema.Modelo.Cita;
import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Servicio.CitaServicio;

import jakarta.servlet.http.HttpSession;

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
    public String mostrarPaginaHistorialCitas(HttpSession session, Model model) {
        logger.info("El usuario ha accedido a la página de historial de citas.");

        try {
            Paciente usuario = (Paciente) session.getAttribute("usuario");
            if (usuario == null) {
                return "redirect:/login";
            }

            // Obtener solo citas del paciente logueado
            List<Cita> citasUsuario = citaServicio.obtenerCitasPorPaciente(usuario.getId());

            model.addAttribute("citasPendientes", citasUsuario);
            model.addAttribute("nombreUsuario", usuario.getNombre());

        } catch (IOException e) {
            logger.error("Error al leer citas", e);
            model.addAttribute("citasPendientes", List.of());
        }

        return "historialCita";
    }

}
