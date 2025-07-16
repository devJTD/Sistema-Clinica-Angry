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

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        logger.info("Intentando cargar usuario por correo electronico: {}", correo);

        Paciente paciente = pacienteRepositorio.findByCorreo(correo)
            .orElseThrow(() -> {
                logger.warn("Fallo en autenticacion: Usuario no encontrado con correo electronico: {}", correo);
                return new UsernameNotFoundException("Usuario no encontrado con correo: " + correo);
            });

        logger.info("Usuario '{}' (ID: {}) cargado exitosamente para autenticacion.", paciente.getCorreo(), paciente.getId());

        return new User(
            paciente.getCorreo(),
            paciente.getContraseña(), // Note: The 'ñ' in 'Contraseña' is a character, not an accent. It remains.
            Collections.emptyList() 
        );
    }
}