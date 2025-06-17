package com.clinica.sistema.Controlador;

import com.clinica.sistema.Servicio.CitaServicio;
import com.clinica.sistema.Servicio.AuthServicio;
import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Modelo.Medico;
import com.clinica.sistema.Modelo.Especialidad;
import com.clinica.sistema.Modelo.Horario;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
public class ReservaControlador {
    private final Logger logger = LoggerFactory.getLogger(ReservaControlador.class);

    private final CitaServicio citaServicio;
    private final AuthServicio authServicio;

    public ReservaControlador(CitaServicio citaServicio, AuthServicio authServicio) {
        this.citaServicio = citaServicio;
        this.authServicio = authServicio;
    }

    private Paciente getPacienteLogueado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() instanceof String) {
            return null;
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String correoUsuario = userDetails.getUsername();
        Optional<Paciente> pacienteOpt = authServicio.buscarPorCorreo(correoUsuario);
        return pacienteOpt.orElse(null);
    }

    @GetMapping("/reserva")
    public String mostrarPaginaReservaCita(Model model) {
        logger.info("Accediendo a la página de reserva de cita.");

        Paciente pacienteLogueado = getPacienteLogueado();
        if (pacienteLogueado != null) {
            model.addAttribute("nombreUsuario", pacienteLogueado.getNombre() + " " + pacienteLogueado.getApellido());
        } else {
            logger.warn("Usuario no autenticado intentó acceder a /reserva.");
            return "redirect:/login?error=nologin";
        }

        return "reservarCita";
    }

    @PostMapping("/reserva/confirmar")
    public String confirmarCita(@RequestParam("fechaCita") String fechaStr,
                                 @RequestParam("horaCita") String horaStr,
                                 @RequestParam("idMedico") Long idMedico,
                                 Model model) {
        logger.info("Intentando crear cita con fecha: {}, hora: {}, idMedico: {}", fechaStr, horaStr, idMedico);

        Paciente pacienteLogueado = getPacienteLogueado();
        if (pacienteLogueado == null || pacienteLogueado.getId() == null) {
            logger.warn("Intento de confirmar cita sin paciente logueado o ID de paciente nulo.");
            return "redirect:/login?error=Sesion expirada o no iniciada. Por favor, vuelve a iniciar sesión.";
        }

        Long idPacienteActual = pacienteLogueado.getId();

        try {
            citaServicio.crearCita(fechaStr, horaStr, idMedico, idPacienteActual);
            logger.info("Cita creada correctamente para paciente ID: {}", idPacienteActual);
            return "redirect:/historial";

        } catch (IllegalArgumentException e) {
            logger.error("Error al confirmar cita (datos inválidos/no encontrados): {}", e.getMessage());
            return "redirect:/reserva?error=" + e.getMessage();
        } catch (IllegalStateException e) {
            logger.error("Error al confirmar cita (lógica de negocio): {}", e.getMessage());
            return "redirect:/reserva?error=" + e.getMessage();
        } catch (Exception e) {
            logger.error("Error inesperado al confirmar cita: {}", e.getMessage(), e);
            return "redirect:/reserva?error=Ha ocurrido un error inesperado al reservar la cita.";
        }
    }

    @GetMapping("/api/especialidades")
    @ResponseBody
    public List<Especialidad> obtenerTodasLasEspecialidades() {
        logger.info("Solicitando todas las especialidades a través de CitaServicio.");
        return citaServicio.obtenerTodasLasEspecialidades();
    }

    @GetMapping("/api/medicos-por-especialidad")
    @ResponseBody
    public List<Medico> obtenerMedicosPorEspecialidad(@RequestParam("idEspecialidad") Long idEspecialidad) {
        logger.info("Solicitando médicos para especialidad ID: {} a través de CitaServicio.", idEspecialidad);
        return citaServicio.obtenerMedicosPorEspecialidad(idEspecialidad);
    }

    @GetMapping("/api/horarios-disponibles")
    @ResponseBody
    public List<Horario> obtenerHorariosDisponibles(
            @RequestParam("idMedico") Long idMedico,
            @RequestParam("fechaCita") String fechaCitaStr) {
        logger.info("Solicitando horarios disponibles para médico ID: {} en fecha: {} a través de CitaServicio.", idMedico, fechaCitaStr);
        LocalDate fecha = LocalDate.parse(fechaCitaStr);
        return citaServicio.obtenerHorariosDisponiblesPorMedicoYFecha(idMedico, fecha);
    }
}