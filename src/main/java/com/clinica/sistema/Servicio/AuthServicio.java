package com.clinica.sistema.Servicio;

import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Repositorio.PacienteRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Service
public class AuthServicio {

    private final PacienteRepositorio pacienteRepositorio;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final Logger logger = LoggerFactory.getLogger(AuthServicio.class);

    public AuthServicio(PacienteRepositorio pacienteRepositorio) {
        this.pacienteRepositorio = pacienteRepositorio;
        logger.info("AuthServicio inicializado con PacienteRepositorio.");
    }

    public boolean existePacientePorEmailODni(String email, String dni) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El correo no puede estar vacío.");
        }
        if (dni == null || dni.isBlank()) {
            throw new IllegalArgumentException("El DNI no puede estar vacío.");
        }

        Optional<Paciente> pacientePorCorreo = pacienteRepositorio.findByCorreo(email);
        Optional<Paciente> pacientePorDni = pacienteRepositorio.findByDni(dni);

        boolean existe = pacientePorCorreo.isPresent() || pacientePorDni.isPresent();

        if (existe) {
            logger.warn("Se encontró un paciente existente con el correo {} o DNI {}.", email, dni);
        } else {
            logger.info("No existe un paciente con el correo {} ni DNI {}. Se puede registrar.", email, dni);
        }
        return existe;
    }

    @Transactional
    public Paciente guardarPaciente(Paciente paciente) {
        if (paciente == null) {
            throw new IllegalArgumentException("El paciente a guardar no puede ser nulo.");
        }
        if (paciente.getNombre() == null || paciente.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del paciente no puede estar vacío.");
        }
        if (paciente.getApellido() == null || paciente.getApellido().isBlank()) {
            throw new IllegalArgumentException("El apellido del paciente no puede estar vacío.");
        }
        if (paciente.getCorreo() == null || paciente.getCorreo().isBlank()) {
            throw new IllegalArgumentException("El correo del paciente no puede estar vacío.");
        }
        if (paciente.getDni() == null || paciente.getDni().isBlank()) {
            throw new IllegalArgumentException("El DNI del paciente no puede estar vacío.");
        }
        if (paciente.getContraseña() == null || paciente.getContraseña().isBlank()) {
            throw new IllegalArgumentException("La contraseña del paciente no puede estar vacía.");
        }

        String hashedPassword = passwordEncoder.encode(paciente.getContraseña());
        paciente.setContraseña(hashedPassword);
        logger.debug("Contraseña del paciente {} encriptada antes de guardar.", paciente.getCorreo());

        Paciente pacienteGuardado = pacienteRepositorio.save(paciente);
        logger.info("Paciente guardado exitosamente con ID {} y correo {}", pacienteGuardado.getId(), pacienteGuardado.getCorreo());
        return pacienteGuardado;
    }

    public Optional<Paciente> buscarPorCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException("El correo no puede estar vacío para la búsqueda.");
        }
        logger.debug("Buscando paciente por correo: {}", correo);
        return pacienteRepositorio.findByCorreo(correo);
    }

    public Optional<Paciente> buscarPacientePorId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID del paciente no puede ser nulo o negativo.");
        }
        return pacienteRepositorio.findById(id);
    }
}