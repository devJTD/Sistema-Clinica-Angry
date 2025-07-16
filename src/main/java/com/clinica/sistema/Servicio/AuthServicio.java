package com.clinica.sistema.Servicio;

import java.util.ArrayList;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clinica.sistema.Modelo.Direccion;
import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Repositorio.PacienteRepositorio;

@Service
public class AuthServicio {

    private final PacienteRepositorio pacienteRepositorio;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final Logger logger = LoggerFactory.getLogger(AuthServicio.class);

    public AuthServicio(PacienteRepositorio pacienteRepositorio) {
        this.pacienteRepositorio = pacienteRepositorio;
    }

    public boolean existePacientePorEmailODni(String email, String dni) {
        logger.debug("Verificando existencia de paciente por correo: {} o DNI: {}", email, dni);
        if (email == null || email.isBlank()) {
            logger.warn("Validacion fallida en existePacientePorEmailODni: El correo no puede estar vacio.");
            throw new IllegalArgumentException("El correo no puede estar vacio.");
        }
        if (dni == null || dni.isBlank()) {
            logger.warn("Validacion fallida en existePacientePorEmailODni: El DNI no puede estar vacio.");
            throw new IllegalArgumentException("El DNI no puede estar vacio.");
        }

        Optional<Paciente> pacientePorCorreo = pacienteRepositorio.findByCorreo(email);
        Optional<Paciente> pacientePorDni = pacienteRepositorio.findByDni(dni);

        boolean existe = pacientePorCorreo.isPresent() || pacientePorDni.isPresent();
        
        if (existe) {
            logger.info("Verificacion de existencia: Se encontro un paciente con correo: {} o DNI: {}. Existe: {}", email, dni, existe);
        } else {
            logger.info("Verificacion de existencia: No se encontro un paciente con correo: {} o DNI: {}. Existe: {}", email, dni, existe);
        }
        return existe;
    }

    @SuppressWarnings("unused")
    @Transactional
    public Paciente guardarPaciente(Paciente paciente, Direccion direccion) {
        logger.info("Intentando guardar nuevo paciente con DNI: {} y correo: {}", paciente.getDni(), paciente.getCorreo());
        if (paciente == null) {
            logger.warn("Validacion fallida en guardarPaciente: El paciente a guardar es nulo.");
            throw new IllegalArgumentException("El paciente a guardar no puede ser nulo.");
        }
        if (paciente.getNombre() == null || paciente.getNombre().isBlank()) {
            logger.warn("Validacion fallida en guardarPaciente para DNI {}: El nombre del paciente no puede estar vacio.", paciente.getDni());
            throw new IllegalArgumentException("El nombre del paciente no puede estar vacio.");
        }
        if (paciente.getApellido() == null || paciente.getApellido().isBlank()) {
            logger.warn("Validacion fallida en guardarPaciente para DNI {}: El apellido del paciente no puede estar vacio.", paciente.getDni());
            throw new IllegalArgumentException("El apellido del paciente no puede estar vacio.");
        }
        if (paciente.getCorreo() == null || paciente.getCorreo().isBlank()) {
            logger.warn("Validacion fallida en guardarPaciente para DNI {}: El correo del paciente no puede estar vacio.", paciente.getDni());
            throw new IllegalArgumentException("El correo del paciente no puede estar vacio.");
        }
        if (paciente.getDni() == null || paciente.getDni().isBlank()) {
            logger.warn("Validacion fallida en guardarPaciente para correo {}: El DNI del paciente no puede estar vacio.", paciente.getCorreo());
            throw new IllegalArgumentException("El DNI del paciente no puede estar vacio.");
        }
        if (paciente.getContraseña() == null || paciente.getContraseña().isBlank()) { // "Contraseña" contains 'ñ', which is a character, not an accent. It stays.
            logger.warn("Validacion fallida en guardarPaciente para DNI {}: La contrasena del paciente no puede estar vacia.", paciente.getDni());
            throw new IllegalArgumentException("La contrasena del paciente no puede estar vacia.");
        }
        
        if (direccion == null || direccion.getDireccionCompleta() == null || direccion.getDireccionCompleta().isBlank()) {
            logger.warn("Validacion fallida en guardarPaciente para DNI {}: La direccion no puede ser nula o vacia.", paciente.getDni());
            throw new IllegalArgumentException("La direccion no puede ser nula o vacia.");
        }

        logger.debug("Encriptando contrasena para el paciente con DNI: {}", paciente.getDni());
        String hashedPassword = passwordEncoder.encode(paciente.getContraseña());
        paciente.setContraseña(hashedPassword);

        if (paciente.getDirecciones() == null) {
            paciente.setDirecciones(new ArrayList<>());
            logger.debug("Inicializando lista de direcciones para el paciente con DNI: {}.", paciente.getDni());
        }

        direccion.setPaciente(paciente);
        paciente.getDirecciones().add(direccion);
        logger.debug("Asignando direccion al paciente con DNI: {}. Direccion: {}", paciente.getDni(), direccion.getDireccionCompleta());

        Paciente pacienteGuardado = pacienteRepositorio.save(paciente);
        logger.info("Paciente con DNI: {} y correo: {} guardado exitosamente con ID: {}.", pacienteGuardado.getDni(), pacienteGuardado.getCorreo(), pacienteGuardado.getId());
        return pacienteGuardado;
    }

    public Optional<Paciente> buscarPorCorreo(String correo) {
        logger.debug("Buscando paciente por correo: {}", correo);
        if (correo == null || correo.isBlank()) {
            logger.warn("Validacion fallida en buscarPorCorreo: El correo no puede estar vacio para la busqueda.");
            throw new IllegalArgumentException("El correo no puede estar vacio para la busqueda.");
        }
        Optional<Paciente> paciente = pacienteRepositorio.findByCorreo(correo);
        if (paciente.isPresent()) {
            logger.info("Paciente encontrado por correo: {} (ID: {}).", correo, paciente.get().getId());
        } else {
            logger.info("Paciente no encontrado por correo: {}.", correo);
        }
        return paciente;
    }

    public Optional<Paciente> buscarPacientePorId(Long id) {
        logger.debug("Buscando paciente por ID: {}", id);
        if (id == null || id <= 0) {
            logger.warn("Validacion fallida en buscarPacientePorId: El ID del paciente no puede ser nulo o negativo. ID: {}", id);
            throw new IllegalArgumentException("El ID del paciente no puede ser nulo o negativo.");
        }
        Optional<Paciente> paciente = pacienteRepositorio.findById(id);
        if (paciente.isPresent()) {
            logger.info("Paciente encontrado por ID: {} (DNI: {}).", id, paciente.get().getDni());
        } else {
            logger.info("Paciente no encontrado por ID: {}.", id);
        }
        return paciente;
    }
}