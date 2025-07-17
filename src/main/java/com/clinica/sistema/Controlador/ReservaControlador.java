package com.clinica.sistema.Controlador;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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

    private static final String MDC_USER_FULL_NAME = "userFullName";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_USER_DNI = "userDni";

    public ReservaControlador(CitaServicio citaServicio, AuthServicio authServicio) {
        this.citaServicio = citaServicio;
        this.authServicio = authServicio;
    }

    private Paciente getPacienteLogueado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            logger.debug("Intento de obtener paciente logueado: No hay autenticacion o el usuario no esta autenticado.");
            return null;
        }

        if (authentication.getPrincipal() instanceof String) {
            logger.debug("Intento de obtener paciente logueado: Principal es String, no UserDetails. Posiblemente usuario anonimo o sin rol.");
            return null;
        }

        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String correoUsuario = userDetails.getUsername();
            logger.debug("Buscando paciente logueado con correo: {}", correoUsuario);
            Optional<Paciente> pacienteOpt = authServicio.buscarPorCorreo(correoUsuario);
            if (pacienteOpt.isPresent()) {
                Paciente paciente = pacienteOpt.get();
                MDC.put(MDC_USER_FULL_NAME, paciente.getNombre() + " " + paciente.getApellido());
                MDC.put(MDC_USER_ID, String.valueOf(paciente.getId()));
                MDC.put(MDC_USER_DNI, paciente.getDni());
                logger.debug("Paciente logueado encontrado con ID: {} y correo: {}", paciente.getId(), correoUsuario);
            } else {
                logger.warn("Paciente logueado no encontrado en la base de datos para el correo: {}", correoUsuario);
            }
            return pacienteOpt.orElse(null);
        } catch (ClassCastException e) {
            logger.error("Error al castear principal a UserDetails: {}", e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            logger.error("Error inesperado al obtener paciente logueado: {}", e.getMessage(), e);
            return null;
        }
    }

    @GetMapping("/reserva")
    public String mostrarPaginaReservaCita(Model model) {
        Paciente pacienteLogueado = getPacienteLogueado();
        if (pacienteLogueado != null) {
            model.addAttribute("nombreUsuario", pacienteLogueado.getNombre() + " " + pacienteLogueado.getApellido());
            logger.info("El usuario {} (ID: {}, DNI: {}) ha accedido a la pagina de reserva de citas.", 
                         MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI));
        } else {
            logger.warn("Usuario no logueado intento acceder a la pagina de reserva de citas, redirigiendo a login.");
            return "redirect:/login?error=nologin";
        }
        return "reservarCita";
    }

    @PostMapping("/reserva/confirmar")
    public String confirmarCita(@RequestParam("fechaCita") String fechaStr,
                                  @RequestParam("horaCita") String horaStr,
                                  @RequestParam("idMedico") Long idMedico,
                                  Model model) {
        Paciente pacienteLogueado = getPacienteLogueado();
        if (pacienteLogueado == null || pacienteLogueado.getId() == null) {
            logger.warn("Intento de confirmar cita por usuario no logueado o sin ID de paciente. Redirigiendo a login.");
            return "redirect:/login?error=Sesion expirada o no iniciada. Por favor, vuelve a iniciar sesion.";
        }

        Long idPacienteActual = pacienteLogueado.getId();
        
        logger.info("Usuario con DNI: {} (ID: {}) intentando confirmar cita con los siguientes datos del formulario: [Fecha: {}, Hora: {}, ID Medico: {}]",
                     MDC.get(MDC_USER_DNI), MDC.get(MDC_USER_ID), fechaStr, horaStr, idMedico);

        try {
            citaServicio.crearCita(fechaStr, horaStr, idMedico, idPacienteActual);
            logger.info("Cita confirmada exitosamente para el usuario {} (ID: {}, DNI: {}) con Medico ID: {} en Fecha: {} Hora: {}.",
                         MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI), idMedico, fechaStr, horaStr);
            return "redirect:/historial";

        } catch (IllegalArgumentException e) {
            logger.error("Error al confirmar cita para el usuario {} (ID: {}, DNI: {}). Causa: {}", 
                         MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI), e.getMessage());
            return "redirect:/reserva?error=" + e.getMessage();
        } catch (IllegalStateException e) {
            logger.error("Estado invalido al confirmar cita para el usuario {} (ID: {}, DNI: {}). Causa: {}", 
                         MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI), e.getMessage());
            return "redirect:/reserva?error=" + e.getMessage();
        } catch (Exception e) {
            logger.error("Error inesperado al confirmar cita para el usuario {} (ID: {}, DNI: {}): {}", 
                         MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI), e.getMessage(), e);
            return "redirect:/reserva?error=Ha ocurrido un error inesperado al reservar la cita.";
        }
    }

    @GetMapping("/api/especialidades")
    @ResponseBody
    public List<Especialidad> obtenerTodasLasEspecialidades() {
        List<Especialidad> especialidades = citaServicio.obtenerTodasLasEspecialidades();
        logger.debug("API: {} (ID: {}) solicitó y se devolvieron {} especialidades.", 
                     MDC.get(MDC_USER_FULL_NAME) != null ? MDC.get(MDC_USER_FULL_NAME) : "Usuario no logueado",
                     MDC.get(MDC_USER_ID) != null ? MDC.get(MDC_USER_ID) : "N/A", 
                     especialidades.size());
        return especialidades;
    }

    @GetMapping("/api/medicos-por-especialidad")
    @ResponseBody
    public List<Medico> obtenerMedicosPorEspecialidad(@RequestParam("idEspecialidad") Long idEspecialidad) {
        logger.debug("API: {} (ID: {}) solicitó medicos para especialidad con ID: {}", 
                     MDC.get(MDC_USER_FULL_NAME) != null ? MDC.get(MDC_USER_FULL_NAME) : "Usuario no logueado",
                     MDC.get(MDC_USER_ID) != null ? MDC.get(MDC_USER_ID) : "N/A", 
                     idEspecialidad);
        List<Medico> medicos = citaServicio.obtenerMedicosPorEspecialidad(idEspecialidad);
        logger.debug("API: {} (ID: {}) recibió {} medicos para la especialidad ID: {}.", 
                     MDC.get(MDC_USER_FULL_NAME) != null ? MDC.get(MDC_USER_FULL_NAME) : "Usuario no logueado",
                     MDC.get(MDC_USER_ID) != null ? MDC.get(MDC_USER_ID) : "N/A", 
                     medicos.size(), idEspecialidad);
        return medicos;
    }

    @GetMapping("/api/horarios-disponibles")
    @ResponseBody
    public List<Horario> obtenerHorariosDisponibles(
            @RequestParam("idMedico") Long idMedico,
            @RequestParam("fechaCita") String fechaCitaStr) {
        LocalDate fecha = LocalDate.parse(fechaCitaStr);
        logger.debug("API: {} (ID: {}) solicitó horarios disponibles para Medico ID: {} en Fecha: {}", 
                     MDC.get(MDC_USER_FULL_NAME) != null ? MDC.get(MDC_USER_FULL_NAME) : "Usuario no logueado",
                     MDC.get(MDC_USER_ID) != null ? MDC.get(MDC_USER_ID) : "N/A", 
                     idMedico, fechaCitaStr);
        List<Horario> horarios = citaServicio.obtenerHorariosDisponiblesPorMedicoYFecha(idMedico, fecha);
        logger.debug("API: {} (ID: {}) recibió {} horarios disponibles para Medico ID: {} en Fecha: {}.", 
                     MDC.get(MDC_USER_FULL_NAME) != null ? MDC.get(MDC_USER_FULL_NAME) : "Usuario no logueado",
                     MDC.get(MDC_USER_ID) != null ? MDC.get(MDC_USER_ID) : "N/A", 
                     horarios.size(), idMedico, fechaCitaStr);
        return horarios;
    }
}