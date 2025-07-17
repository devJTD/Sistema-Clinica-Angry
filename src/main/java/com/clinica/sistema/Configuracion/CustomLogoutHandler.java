package com.clinica.sistema.Configuracion; // Puedes ajustar el paquete

import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Servicio.AuthServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;

@Component
public class CustomLogoutHandler implements LogoutHandler {

    private final Logger logger = LoggerFactory.getLogger(CustomLogoutHandler.class);
    private final AuthServicio authServicio;

    // Constantes MDC para consistencia
    private static final String MDC_USER_FULL_NAME = "userFullName";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_USER_DNI = "userDni";

    public CustomLogoutHandler(AuthServicio authServicio) {
        this.authServicio = authServicio;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String correoUsuario = userDetails.getUsername();

            Optional<Paciente> pacienteOpt = authServicio.buscarPorCorreo(correoUsuario);

            // Intentar establecer el MDC justo antes del logout
            if (pacienteOpt.isPresent()) {
                Paciente paciente = pacienteOpt.get();
                MDC.put(MDC_USER_FULL_NAME, paciente.getNombre() + " " + paciente.getApellido());
                MDC.put(MDC_USER_ID, String.valueOf(paciente.getId()));
                MDC.put(MDC_USER_DNI, paciente.getDni());
                logger.info("El usuario {} (ID: {}, DNI: {}) ha cerrado sesion.",
                        MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI));
            } else {
                logger.warn("El usuario '{}' ha cerrado sesion, pero no se encontro informacion de paciente en la base de datos para el log.", correoUsuario);
            }
        } else {
            logger.info("Un usuario no autenticado o con informacion incompleta ha cerrado sesion.");
        }

        // Importante: Limpiar el MDC despues de usarlo
        MDC.remove(MDC_USER_FULL_NAME);
        MDC.remove(MDC_USER_ID);
        MDC.remove(MDC_USER_DNI);
    }
}