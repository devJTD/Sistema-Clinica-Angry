package com.clinica.sistema.Controlador;

import com.clinica.sistema.Servicio.CitaServicio;
import com.clinica.sistema.Modelo.Paciente; 

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.StringUtils; 
import com.google.common.base.Preconditions; 

@Controller
public class ReservaControlador {
    private Logger logger = LoggerFactory.getLogger(ReservaControlador.class);

    private final CitaServicio citaServicio;
    
    public ReservaControlador(CitaServicio citaServicio) {
        this.citaServicio = citaServicio;
    }

    @GetMapping("/reserva")
    public String mostrarPaginaReservaCita() {
        logger.info("Accediendo a la página de reserva de cita.");
        return "reservarCita";
    }

    @PostMapping("/reserva/confirmar")
    public String confirmarCita(@RequestParam("fechaCita") String fecha,
                                @RequestParam("horaCita") String hora,
                                @RequestParam("idMedico") Long idMedico,
                                HttpSession session, Model model) {
        logger.info("Intentando crear cita con fecha: {}, hora: {}, idMedico: {}", fecha, hora, idMedico);

        Paciente pacienteLogueado = (Paciente) session.getAttribute("usuario");
        if (pacienteLogueado == null) {
            logger.warn("Intento de confirmar cita sin paciente logueado.");
            return "redirect:/login?error=nologin"; 
        }
        Long idPacienteActual = pacienteLogueado.getId();

        try {
            Preconditions.checkArgument(StringUtils.isNotBlank(fecha), "La fecha de la cita no puede estar vacía.");
            Preconditions.checkArgument(StringUtils.isNotBlank(hora), "La hora de la cita no puede estar vacía.");
            Preconditions.checkNotNull(idMedico, "El ID del médico no puede ser nulo.");
            Preconditions.checkArgument(idMedico > 0, "El ID del médico debe ser un valor positivo.");
            Preconditions.checkNotNull(idPacienteActual, "El ID del paciente no pudo ser determinado.");

            citaServicio.crearCita(fecha, hora, idMedico, session, model);
            logger.info("Cita creada correctamente");
            return "redirect:/historial?success";

        } catch (IllegalArgumentException e) {
            logger.error("Error de validación al crear cita: {}", e.getMessage());
            return "redirect:/reserva?error=" + e.getMessage();
        } catch (IOException e) {
            logger.error("Error de E/S al crear cita: {}", e.getMessage(), e);
            return "redirect:/reserva?error=Error al guardar la cita.";
        }
    }
}