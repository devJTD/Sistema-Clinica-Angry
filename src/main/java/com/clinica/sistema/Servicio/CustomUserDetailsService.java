package com.clinica.sistema.Servicio;

import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Repositorio.PacienteRepositorio;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final PacienteRepositorio pacienteRepositorio;

    public CustomUserDetailsService(PacienteRepositorio pacienteRepositorio) {
        this.pacienteRepositorio = pacienteRepositorio;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Paciente paciente = pacienteRepositorio.findByCorreo(correo)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con correo: " + correo));

        return new User(
            paciente.getCorreo(),
            paciente.getContraseña(),
            Collections.emptyList()
        );
    }
}