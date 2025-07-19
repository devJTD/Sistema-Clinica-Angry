package com.clinica.sistema.Controlador;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.clinica.sistema.Modelo.Especialidad;
import com.clinica.sistema.Modelo.Horario;
import com.clinica.sistema.Modelo.Medico;
import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Servicio.AuthServicio;
import com.clinica.sistema.Servicio.CitaServicio;

@Controller
public class ReservaControlador {
    private final Logger logger = LoggerFactory.getLogger(ReservaControlador.class);

    private final CitaServicio citaServicio;
    private final AuthServicio authServicio;

    public ReservaControlador(CitaServicio citaServicio, AuthServicio authServicio) {
        this.citaServicio = citaServicio;
        this.authServicio = authServicio;
    }

    // Método auxiliar para obtener el objeto Paciente del usuario actualmente logueado.
    private Paciente getPacienteLogueado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Verifica si hay autenticación o si el usuario no está autenticado.
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.debug("Intento de obtener paciente logueado: No hay autenticacion o el usuario no esta autenticado.");
            return null;
        }

        // Verifica si el principal (usuario) es una cadena, lo que podría indicar un usuario anónimo.
        if (authentication.getPrincipal() instanceof String) {
            logger.debug("Intento de obtener paciente logueado: Principal es String, no UserDetails. Posiblemente usuario anonimo o sin rol.");
            return null;
        }

        try {
            // Obtiene los detalles del usuario y su correo electrónico.
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String correoUsuario = userDetails.getUsername();
            logger.debug("Buscando paciente logueado con correo: {}", correoUsuario);
            // Busca el paciente en la base de datos por su correo.
            Optional<Paciente> pacienteOpt = authServicio.buscarPorCorreo(correoUsuario);
            if (pacienteOpt.isPresent()) {
                logger.debug("Paciente logueado encontrado con ID: {} y correo: {}", pacienteOpt.get().getId(), correoUsuario);
            } else {
                logger.warn("Paciente logueado no encontrado en la base de datos para el correo: {}", correoUsuario);
            }
            // Retorna el paciente si está presente, de lo contrario, null.
            return pacienteOpt.orElse(null);
        } catch (ClassCastException e) {
            logger.error("Error al castear principal a UserDetails: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Error inesperado al obtener paciente logueado: {}", e.getMessage(), e);
            return null;
        }
    }

    // Muestra la página de reserva de citas.
    @GetMapping("/reserva")
    public String mostrarPaginaReservaCita(Model model) {
        // Obtiene el paciente logueado.
        Paciente pacienteLogueado = getPacienteLogueado();
        // Si hay un paciente logueado, añade su nombre completo al modelo.
        if (pacienteLogueado != null) {
            model.addAttribute("nombreUsuario", pacienteLogueado.getNombre() + " " + pacienteLogueado.getApellido());
            logger.info("Usuario con DNI: {} ha accedido a la pagina de reserva de citas.", pacienteLogueado.getDni());
        } else {
            // Si no hay un paciente logueado, redirige a la página de login.
            logger.warn("Usuario no logueado intento acceder a la pagina de reserva de citas, redirigiendo a login.");
            return "redirect:/login?error=nologin";
        }
        return "reservarCita";
    }

    // Procesa la confirmación de una cita enviada desde el formulario.
    @PostMapping("/reserva/confirmar")
    public String confirmarCita(@RequestParam("fechaCita") String fechaStr,
                                  @RequestParam("horaCita") String horaStr,
                                  @RequestParam("idMedico") Long idMedico,
                                  Model model) {
        // Obtiene el paciente logueado.
        Paciente pacienteLogueado = getPacienteLogueado();
        // Redirige si el paciente no está logueado o no tiene ID.
        if (pacienteLogueado == null || pacienteLogueado.getId() == null) {
            logger.warn("Intento de confirmar cita por usuario no logueado o sin ID de paciente. Redirigiendo a login.");
            return "redirect:/login?error=Sesion expirada o no iniciada. Por favor, vuelve a iniciar sesion.";
        }

        Long idPacienteActual = pacienteLogueado.getId();
        
        logger.info("Usuario con DNI: {} ({}) intentando confirmar cita con los siguientes datos del formulario: [Fecha: {}, Hora: {}, ID Medico: {}]",
                     pacienteLogueado.getDni(), pacienteLogueado.getCorreo(), fechaStr, horaStr, idMedico);

        try {
            // Llama al servicio para crear la cita.
            citaServicio.crearCita(fechaStr, horaStr, idMedico, idPacienteActual);
            logger.info("Cita confirmada exitosamente para el usuario con DNI: {} con Medico ID: {} en Fecha: {} Hora: {}.",
                         pacienteLogueado.getDni(), idMedico, fechaStr, horaStr);
            // Redirige al historial de citas tras la confirmación exitosa.
            return "redirect:/historial";

        } catch (IllegalArgumentException e) {
            // Captura y maneja errores de argumentos inválidos.
            logger.error("Error al confirmar cita para usuario con DNI: {}. Causa: {}", pacienteLogueado.getDni(), e.getMessage());
            return "redirect:/reserva?error=" + e.getMessage();
        } catch (IllegalStateException e) {
            // Captura y maneja errores de estado inválido.
            logger.error("Estado invalido al confirmar cita para usuario con DNI: {}. Causa: {}", pacienteLogueado.getDni(), e.getMessage());
            return "redirect:/reserva?error=" + e.getMessage();
        } catch (Exception e) {
            // Captura cualquier otro error inesperado.
            logger.error("Error inesperado al confirmar cita para usuario con DNI: {}: {}", pacienteLogueado.getDni(), e.getMessage(), e);
            return "redirect:/reserva?error=Ha ocurrido un error inesperado al reservar la cita.";
        }
    }

    // Endpoint API para obtener todas las especialidades disponibles.
    @GetMapping("/api/especialidades")
    @ResponseBody
    public List<Especialidad> obtenerTodasLasEspecialidades() {
        // Obtiene todas las especialidades.
        List<Especialidad> especialidades = citaServicio.obtenerTodasLasEspecialidades();
        logger.debug("API: Se solicitaron y se devolvieron {} especialidades.", especialidades.size());
        return especialidades;
    }

    // Endpoint API para obtener médicos filtrados por especialidad.
    @GetMapping("/api/medicos-por-especialidad")
    @ResponseBody
    public List<Medico> obtenerMedicosPorEspecialidad(@RequestParam("idEspecialidad") Long idEspecialidad) {
        logger.debug("API: Solicitud de medicos para especialidad con ID: {}", idEspecialidad);
        // Obtiene los médicos de la especialidad solicitada.
        List<Medico> medicos = citaServicio.obtenerMedicosPorEspecialidad(idEspecialidad);
        logger.debug("API: Se devolvieron {} medicos para la especialidad ID: {}.", medicos.size(), idEspecialidad);
        return medicos;
    }

    // Endpoint API para obtener horarios disponibles de un médico en una fecha específica.
    @GetMapping("/api/horarios-disponibles")
    @ResponseBody
    public List<Horario> obtenerHorariosDisponibles(
            @RequestParam("idMedico") Long idMedico,
            @RequestParam("fechaCita") String fechaCitaStr) {
        // Parsea la fecha de String a LocalDate.
        LocalDate fecha = LocalDate.parse(fechaCitaStr);
        logger.debug("API: Solicitud de horarios disponibles para Medico ID: {} en Fecha: {}", idMedico, fechaCitaStr);
        // Obtiene los horarios disponibles para el médico y fecha especificados.
        List<Horario> horarios = citaServicio.obtenerHorariosDisponiblesPorMedicoYFecha(idMedico, fecha);
        logger.debug("API: Se devolvieron {} horarios disponibles para Medico ID: {} en Fecha: {}.", horarios.size(), idMedico, fechaCitaStr);
        return horarios;
    }
}