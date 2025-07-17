package com.clinica.sistema.Controlador;

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

import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Servicio.AuthServicio;

@Controller
public class InicioControlador {

    private final Logger logger = LoggerFactory.getLogger(AuthControlador.class);

    private final AuthServicio authServicio;

    private static final String MDC_USER_FULL_NAME = "userFullName";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_USER_DNI = "userDni";

    public InicioControlador(AuthServicio authServicio) {
        this.authServicio = authServicio;
    }

    private Paciente getPacienteLogueado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() instanceof String) {
            logger.debug("No hay usuario autenticado o el principal no es un UserDetails.");
            return null;
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userEmail = userDetails.getUsername();
        logger.debug("Intentando buscar paciente con correo: {}", userEmail);

        Optional<Paciente> patientOptional = authServicio.buscarPorCorreo(userEmail);
        if (patientOptional.isPresent()) {
            logger.info("Paciente encontrado para el correo: {}", userEmail);
        } else {
            logger.warn("No se encontro paciente en la base de datos para el correo: {}", userEmail);
        }
        return patientOptional.orElse(null);
    }

    @GetMapping("/")
    public String mostrarPaginaInicio(Model model) {
        Paciente loggedInPatient = getPacienteLogueado();

        if (loggedInPatient != null) {
            MDC.put(MDC_USER_FULL_NAME, loggedInPatient.getNombre() + " " + loggedInPatient.getApellido());
            MDC.put(MDC_USER_ID, String.valueOf(loggedInPatient.getId()));
            MDC.put(MDC_USER_DNI, loggedInPatient.getDni());
            
            model.addAttribute("nombreCompleto", loggedInPatient.getNombre() + " " + loggedInPatient.getApellido());
            logger.info("El usuario {} (ID: {}, DNI: {}) ha accedido a la pagina de inicio.", 
                         MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI));
        } else {
            logger.info("Usuario no logueado ha accedido a la pagina de inicio.");
        }

        return "inicio";
    }
}