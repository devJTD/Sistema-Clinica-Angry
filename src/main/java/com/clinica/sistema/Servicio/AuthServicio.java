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

    // Verifica la existencia de un paciente por correo electronico o DNI.
    public boolean existePacientePorEmailODni(String email, String dni) {
        logger.debug("Verificando existencia de paciente por correo: {} o DNI: {}", email, dni);
        // Lanza una excepcion si el correo es nulo o vacio.
        if (email == null || email.isBlank()) {
            logger.warn("Validacion fallida en existePacientePorEmailODni: El correo no puede estar vacio.");
            throw new IllegalArgumentException("El correo no puede estar vacio.");
        }
        // Lanza una excepcion si el DNI es nulo o vacio.
        if (dni == null || dni.isBlank()) {
            logger.warn("Validacion fallida en existePacientePorEmailODni: El DNI no puede estar vacio.");
            throw new IllegalArgumentException("El DNI no puede estar vacio.");
        }

        // Busca el paciente por correo y DNI.
        Optional<Paciente> pacientePorCorreo = pacienteRepositorio.findByCorreo(email);
        Optional<Paciente> pacientePorDni = pacienteRepositorio.findByDni(dni);

        // Retorna verdadero si el paciente existe por correo o DNI.
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
        
        // Se mantiene la validacion si el objeto paciente completo es nulo.
        if (paciente == null) {
            logger.warn("Validacion fallida en guardarPaciente: El paciente a guardar es nulo.");
            throw new IllegalArgumentException("El paciente a guardar no puede ser nulo.");
        }
        
        // Se mantiene la validacion si la direccion es nula o vacia, ya que se pasa como un parametro separado.
        if (direccion == null || direccion.getDireccionCompleta() == null || direccion.getDireccionCompleta().isBlank()) {
            logger.warn("Validacion fallida en guardarPaciente para DNI {}: La direccion no puede ser nula o vacia.", paciente.getDni());
            throw new IllegalArgumentException("La direccion no puede ser nula o vacia.");
        }

        // Encripta la contrasena del paciente antes de guardarla.
        logger.debug("Encriptando contrasena para el paciente con DNI: {}", paciente.getDni());
        String hashedPassword = passwordEncoder.encode(paciente.getContraseña());
        paciente.setContraseña(hashedPassword);

        // Inicializa la lista de direcciones si es nula.
        if (paciente.getDirecciones() == null) {
            paciente.setDirecciones(new ArrayList<>());
            logger.debug("Inicializando lista de direcciones para el paciente con DNI: {}.", paciente.getDni());
        }

        // Asocia la direccion al paciente y la anade a la lista de direcciones.
        direccion.setPaciente(paciente);
        paciente.getDirecciones().add(direccion);
        logger.debug("Asignando direccion al paciente con DNI: {}. Direccion: {}", paciente.getDni(), direccion.getDireccionCompleta());

        // Guarda el paciente en el repositorio.
        Paciente pacienteGuardado = pacienteRepositorio.save(paciente);
        logger.info("Paciente con DNI: {} y correo: {} guardado exitosamente con ID: {}.", pacienteGuardado.getDni(), pacienteGuardado.getCorreo(), pacienteGuardado.getId());
        return pacienteGuardado;
    }

    // Busca un paciente por su correo electronico.
    public Optional<Paciente> buscarPorCorreo(String correo) {
        logger.debug("Buscando paciente por correo: {}", correo);
        // Lanza una excepcion si el correo es nulo o vacio.
        if (correo == null || correo.isBlank()) {
            logger.warn("Validacion fallida en buscarPorCorreo: El correo no puede estar vacio para la busqueda.");
            throw new IllegalArgumentException("El correo no puede estar vacio para la busqueda.");
        }
        // Obtiene el paciente del repositorio.
        Optional<Paciente> paciente = pacienteRepositorio.findByCorreo(correo);
        if (paciente.isPresent()) {
            logger.info("Paciente encontrado por correo: {} (ID: {}).", correo, paciente.get().getId());
        } else {
            logger.info("Paciente no encontrado por correo: {}.", correo);
        }
        return paciente;
    }

    // Busca un paciente por su ID.
    public Optional<Paciente> buscarPacientePorId(Long id) {
        logger.debug("Buscando paciente por ID: {}", id);
        // Lanza una excepcion si el ID es nulo o no valido.
        if (id == null || id <= 0) {
            logger.warn("Validacion fallida en buscarPacientePorId: El ID del paciente no puede ser nulo o negativo. ID: {}", id);
            throw new IllegalArgumentException("El ID del paciente no puede ser nulo o negativo.");
        }
        // Obtiene el paciente del repositorio.
        Optional<Paciente> paciente = pacienteRepositorio.findById(id);
        if (paciente.isPresent()) {
            logger.info("Paciente encontrado por ID: {} (DNI: {}).", id, paciente.get().getDni());
        } else {
            logger.info("Paciente no encontrado por ID: {}.", id);
        }
        return paciente;
    }
}