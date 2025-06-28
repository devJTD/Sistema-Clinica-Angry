package com.clinica.sistema.Controlador;

import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Servicio.AuthServicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

@Controller
public class InicioControlador {

    private Logger logger = LoggerFactory.getLogger(InicioControlador.class);

    private final AuthServicio authServicio;

    public InicioControlador(AuthServicio authServicio) {
        this.authServicio = authServicio;
    }

    private Paciente getPacienteLogueado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() instanceof String) {
            return null;
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userEmail = userDetails.getUsername();

        Optional<Paciente> patientOptional = authServicio.buscarPorCorreo(userEmail);
        return patientOptional.orElse(null);
    }

    @GetMapping("/")
    public String mostrarPaginaInicio(Model model) {
        logger.info("El usuario ha accedido a la página de inicio.");

        Paciente loggedInPatient = getPacienteLogueado();

        if (loggedInPatient != null) {
            // *** CAMBIO AQUÍ: Usamos "nombreCompleto" para que coincida con tu HTML ***
            model.addAttribute("nombreCompleto", loggedInPatient.getNombre() + " " + loggedInPatient.getApellido());
            logger.info("Usuario {} autenticado en la página de inicio.", loggedInPatient.getCorreo());
        } else {
            logger.info("Acceso a la página de inicio por usuario no autenticado.");
        }

        return "inicio";
    }
}