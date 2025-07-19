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

    // Método auxiliar para obtener el objeto Paciente del usuario actualmente autenticado.
    private Paciente getPacienteLogueado() {
        // Obtiene el objeto de autenticación del contexto de seguridad de Spring.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Verifica si hay un usuario autenticado y si el principal es un UserDetails (no una cadena anónima).
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() instanceof String) {
            logger.debug("No hay usuario autenticado o el principal no es un UserDetails.");
            return null;
        }

        // Obtiene los detalles del usuario autenticado.
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        // Obtiene el correo electrónico del usuario, que se usa como nombre de usuario.
        String userEmail = userDetails.getUsername();
        logger.debug("Intentando buscar paciente con correo: {}", userEmail);

        // Busca el paciente en la base de datos usando el correo electrónico.
        Optional<Paciente> patientOptional = authServicio.buscarPorCorreo(userEmail);
        if (patientOptional.isPresent()) {
            logger.info("Paciente encontrado para el correo: {}", userEmail);
        } else {
            logger.warn("No se encontro paciente en la base de datos para el correo: {}", userEmail);
        }
        // Retorna el paciente si se encuentra, de lo contrario, retorna null.
        return patientOptional.orElse(null);
    }

    // Muestra la página de inicio de la aplicación.
    // Añade el nombre completo del paciente logueado al modelo si existe.
    @GetMapping("/")
    public String mostrarPaginaInicio(Model model) {
        // Intenta obtener el paciente actualmente logueado.
        Paciente loggedInPatient = getPacienteLogueado();

        // Si hay un paciente logueado, añade su nombre completo al modelo.
        if (loggedInPatient != null) {
            model.addAttribute("nombreCompleto", loggedInPatient.getNombre() + " " + loggedInPatient.getApellido());
            logger.info("Usuario con DNI: {} ({}) ha accedido a la pagina de inicio.", loggedInPatient.getDni(), loggedInPatient.getCorreo());
        } else {
            // Registra que un usuario no logueado ha accedido a la página.
            logger.info("Usuario no logueado ha accedido a la pagina de inicio.");
        }

        // Retorna el nombre de la vista (página HTML) a mostrar.
        return "inicio";
    }
}