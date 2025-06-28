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
        logger.info("[ReservaControlador] - Controlador inicializado con CitaServicio y AuthServicio.");
    }

    private Paciente getPacienteLogueado() {
        logger.debug("[ReservaControlador] - Intentando obtener paciente logueado.");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            logger.debug("[ReservaControlador] - No hay autenticación o el usuario no está autenticado.");
            return null;
        }

        if (authentication.getPrincipal() instanceof String) {
            // Esto puede ocurrir si es "anonymousUser" o si el principal es solo el nombre de usuario
            logger.warn("[ReservaControlador] - Principal de autenticación es String ({}). Puede ser usuario anónimo o no un UserDetails completo.", authentication.getPrincipal());
            return null;
        }

        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String correoUsuario = userDetails.getUsername();
            logger.info("[ReservaControlador] - Usuario autenticado detectado: {}. Buscando paciente en base de datos.", correoUsuario);
            Optional<Paciente> pacienteOpt = authServicio.buscarPorCorreo(correoUsuario);
            if (pacienteOpt.isPresent()) {
                logger.debug("[ReservaControlador] - Paciente logueado encontrado: ID {}, Correo: {}.", pacienteOpt.get().getId(), pacienteOpt.get().getCorreo());
                return pacienteOpt.get();
            } else {
                logger.warn("[ReservaControlador] - Paciente no encontrado en la base de datos para el correo: {}.", correoUsuario);
                return null;
            }
        } catch (ClassCastException e) {
            logger.error("[ReservaControlador] - Error de casteo al obtener UserDetails: {}. Principal type: {}", e.getMessage(), authentication.getPrincipal().getClass().getName());
            return null;
        } catch (Exception e) {
            logger.error("[ReservaControlador] - Error inesperado al obtener paciente logueado: {}", e.getMessage(), e);
            return null;
        }
    }

    @GetMapping("/reserva")
    public String mostrarPaginaReservaCita(Model model) {
        logger.info("[ReservaControlador] - Accediendo a la página de reserva de cita (/reserva).");

        Paciente pacienteLogueado = getPacienteLogueado();
        if (pacienteLogueado != null) {
            logger.info("[ReservaControlador] - Paciente logueado {} {} reconocido para la página de reserva.", pacienteLogueado.getNombre(), pacienteLogueado.getApellido());
            model.addAttribute("nombreUsuario", pacienteLogueado.getNombre() + " " + pacienteLogueado.getApellido());
        } else {
            logger.warn("[ReservaControlador] - Usuario no autenticado o no encontrado intentó acceder a /reserva. Redireccionando a login.");
            return "redirect:/login?error=nologin";
        }

        return "reservarCita";
    }

    @PostMapping("/reserva/confirmar")
    public String confirmarCita(@RequestParam("fechaCita") String fechaStr,
                                 @RequestParam("horaCita") String horaStr,
                                 @RequestParam("idMedico") Long idMedico,
                                 Model model) {
        logger.info("[ReservaControlador] - INICIO: Solicitud POST para confirmar cita. Fecha: {}, Hora: {}, ID Médico: {}.", fechaStr, horaStr, idMedico);

        Paciente pacienteLogueado = getPacienteLogueado();
        if (pacienteLogueado == null || pacienteLogueado.getId() == null) {
            logger.warn("[ReservaControlador] - Intento de confirmar cita sin paciente logueado o con ID de paciente nulo. Redireccionando a login.");
            return "redirect:/login?error=Sesion expirada o no iniciada. Por favor, vuelve a iniciar sesión.";
        }
        logger.debug("[ReservaControlador] - Paciente logueado para confirmar cita: ID {}.", pacienteLogueado.getId());

        Long idPacienteActual = pacienteLogueado.getId();

        try {
            logger.info("[ReservaControlador] - Llamando a CitaServicio.crearCita con fecha: {}, hora: {}, idMedico: {}, idPaciente: {}.", fechaStr, horaStr, idMedico, idPacienteActual);
            citaServicio.crearCita(fechaStr, horaStr, idMedico, idPacienteActual);
            logger.info("[ReservaControlador] - Cita creada correctamente en el servicio para paciente ID: {}. Redireccionando a /historial.", idPacienteActual);
            return "redirect:/historial";

        } catch (IllegalArgumentException e) {
            logger.error("[ReservaControlador] - Error al confirmar cita (datos inválidos o no encontrados): {}", e.getMessage());
            return "redirect:/reserva?error=" + e.getMessage();
        } catch (IllegalStateException e) {
            logger.error("[ReservaControlador] - Error al confirmar cita (lógica de negocio/estado inválido): {}", e.getMessage());
            return "redirect:/reserva?error=" + e.getMessage();
        } catch (Exception e) {
            logger.error("[ReservaControlador] - ERROR INESPERADO al confirmar cita: {}", e.getMessage(), e);
            return "redirect:/reserva?error=Ha ocurrido un error inesperado al reservar la cita.";
        }
    }

    @GetMapping("/api/especialidades")
    @ResponseBody
    public List<Especialidad> obtenerTodasLasEspecialidades() {
        logger.info("[ReservaControlador] - Solicitud API: Obtener todas las especialidades.");
        List<Especialidad> especialidades = citaServicio.obtenerTodasLasEspecialidades();
        logger.debug("[ReservaControlador] - Retornando {} especialidades.", especialidades.size());
        return especialidades;
    }

    @GetMapping("/api/medicos-por-especialidad")
    @ResponseBody
    public List<Medico> obtenerMedicosPorEspecialidad(@RequestParam("idEspecialidad") Long idEspecialidad) {
        logger.info("[ReservaControlador] - Solicitud API: Obtener médicos para especialidad ID: {}.", idEspecialidad);
        List<Medico> medicos = citaServicio.obtenerMedicosPorEspecialidad(idEspecialidad);
        logger.debug("[ReservaControlador] - Retornando {} médicos para especialidad ID {}.", medicos.size(), idEspecialidad);
        return medicos;
    }

    @GetMapping("/api/horarios-disponibles")
    @ResponseBody
    public List<Horario> obtenerHorariosDisponibles(
            @RequestParam("idMedico") Long idMedico,
            @RequestParam("fechaCita") String fechaCitaStr) {
        logger.info("[ReservaControlador] - Solicitud API: Obtener horarios disponibles para médico ID: {} en fecha: {}.", idMedico, fechaCitaStr);
        LocalDate fecha = LocalDate.parse(fechaCitaStr);
        List<Horario> horarios = citaServicio.obtenerHorariosDisponiblesPorMedicoYFecha(idMedico, fecha);
        logger.debug("[ReservaControlador] - Retornando {} horarios para médico ID {} en fecha {}.", horarios.size(), idMedico, fechaCitaStr);
        return horarios;
    }
}