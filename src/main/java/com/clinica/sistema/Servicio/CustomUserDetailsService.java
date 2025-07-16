package com.clinica.sistema.Servicio;

import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Repositorio.PacienteRepositorio;
import org.slf4j.Logger; 
import org.slf4j.LoggerFactory; 
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class); 

    private final PacienteRepositorio pacienteRepositorio;

    public CustomUserDetailsService(PacienteRepositorio pacienteRepositorio) {
        this.pacienteRepositorio = pacienteRepositorio;
    }

    // Carga los detalles de un usuario para autenticación por su correo electrónico.
    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        logger.info("Intentando cargar usuario por correo electronico: {}", correo);

        // Busca el paciente en el repositorio por su correo electrónico.
        Paciente paciente = pacienteRepositorio.findByCorreo(correo)
            // Si el paciente no se encuentra, lanza una excepción UsernameNotFoundException.
            .orElseThrow(() -> {
                logger.warn("Fallo en autenticacion: Usuario no encontrado con correo electronico: {}", correo);
                return new UsernameNotFoundException("Usuario no encontrado con correo: " + correo);
            });

        logger.info("Usuario '{}' (ID: {}) cargado exitosamente para autenticacion.", paciente.getCorreo(), paciente.getId());

        // Retorna un objeto UserDetails de Spring Security con el correo, contraseña y sin roles específicos.
        return new User(
            paciente.getCorreo(),
            paciente.getContraseña(), 
            Collections.emptyList() 
        );
    }
}