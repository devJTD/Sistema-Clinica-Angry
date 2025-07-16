package com.clinica.sistema.Controlador;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final Logger logger = LoggerFactory.getLogger(AuthServicio.class);

    private final AuthServicio authServicio;

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
            model.addAttribute("nombreCompleto", loggedInPatient.getNombre() + " " + loggedInPatient.getApellido());
            logger.info("Usuario con DNI: {} ({}) ha accedido a la pagina de inicio.", loggedInPatient.getDni(), loggedInPatient.getCorreo());
        } else {
            logger.info("Usuario no logueado ha accedido a la pagina de inicio.");
        }

        return "inicio";
    }
}