package com.clinica.sistema.Servicio;

import java.util.ArrayList;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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

    // Constantes para las claves MDC
    private static final String MDC_USER_FULL_NAME = "userFullName";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_USER_DNI = "userDni";

    public AuthServicio(PacienteRepositorio pacienteRepositorio) {
        this.pacienteRepositorio = pacienteRepositorio;
    }

    // Verifica la existencia de un paciente por correo electronico o DNI.
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
            // Si el paciente existe, puedes cargar parte de su información en el MDC
            // Aunque para este método, el paciente aún no está "logueado" o completamente identificado.
            // Es más para propósitos de verificación de existencia.
            // Si se necesitara identificar al paciente que *ya existe*, se haría después de la autenticación.
            logger.info("Verificacion de existencia: Se encontro un paciente con correo: {} o DNI: {}. Existe: {}", email, dni, existe);
        } else {
            logger.info("Verificacion de existencia: No se encontro un paciente con correo: {} o DNI: {}. Existe: {}", email, dni, existe);
        }
        return existe;
    }

    @SuppressWarnings("unused")
    @Transactional
    public Paciente guardarPaciente(Paciente paciente, Direccion direccion) {
        // Antes de la operación, si el paciente no tiene ID, asumimos que es nuevo registro.
        // Después de guardar, ya tendrá ID. Aquí el MDC es más útil para el paciente recién creado.
        // Por ahora, solo tenemos DNI y correo para el log inicial.
        logger.info("Intentando guardar nuevo paciente con DNI: {} y correo: {}", paciente.getDni(), paciente.getCorreo());
        
        if (paciente == null) {
            logger.warn("Validacion fallida en guardarPaciente: El paciente a guardar es nulo.");
            throw new IllegalArgumentException("El paciente a guardar no puede ser nulo.");
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
        
        // Una vez que el paciente es guardado y tiene un ID, podemos poner sus datos en el MDC
        MDC.put(MDC_USER_FULL_NAME, pacienteGuardado.getNombre() + " " + pacienteGuardado.getApellido());
        MDC.put(MDC_USER_ID, String.valueOf(pacienteGuardado.getId()));
        MDC.put(MDC_USER_DNI, pacienteGuardado.getDni());

        logger.info("Paciente con DNI: {} y correo: {} guardado exitosamente con ID: {}. Accion realizada por el usuario registrado.", 
                    pacienteGuardado.getDni(), pacienteGuardado.getCorreo(), pacienteGuardado.getId());
        return pacienteGuardado;
    }

    // Busca un paciente por su correo electronico.
    public Optional<Paciente> buscarPorCorreo(String correo) {
        logger.debug("Buscando paciente por correo: {}", correo);
        if (correo == null || correo.isBlank()) {
            logger.warn("Validacion fallida en buscarPorCorreo: El correo no puede estar vacio para la busqueda.");
            throw new IllegalArgumentException("El correo no puede estar vacio para la busqueda.");
        }
        Optional<Paciente> paciente = pacienteRepositorio.findByCorreo(correo);
        if (paciente.isPresent()) {
            // Si el paciente se encuentra, podemos actualizar el MDC si aún no está establecido
            // o si esta operación es parte de un flujo de login/identificación.
            Paciente foundPaciente = paciente.get();
            MDC.put(MDC_USER_FULL_NAME, foundPaciente.getNombre() + " " + foundPaciente.getApellido());
            MDC.put(MDC_USER_ID, String.valueOf(foundPaciente.getId()));
            MDC.put(MDC_USER_DNI, foundPaciente.getDni());
            logger.info("Paciente encontrado por correo: {} (ID: {}).", correo, foundPaciente.getId());
        } else {
            logger.info("Paciente no encontrado por correo: {}.", correo);
        }
        return paciente;
    }

    // Busca un paciente por su ID.
    public Optional<Paciente> buscarPacientePorId(Long id) {
        logger.debug("Buscando paciente por ID: {}", id);
        if (id == null || id <= 0) {
            logger.warn("Validacion fallida en buscarPacientePorId: El ID del paciente no puede ser nulo o negativo. ID: {}", id);
            throw new IllegalArgumentException("El ID del paciente no puede ser nulo o negativo.");
        }
        Optional<Paciente> paciente = pacienteRepositorio.findById(id);
        if (paciente.isPresent()) {
            // Similar a buscarPorCorreo, actualizamos el MDC
            Paciente foundPaciente = paciente.get();
            MDC.put(MDC_USER_FULL_NAME, foundPaciente.getNombre() + " " + foundPaciente.getApellido());
            MDC.put(MDC_USER_ID, String.valueOf(foundPaciente.getId()));
            MDC.put(MDC_USER_DNI, foundPaciente.getDni());
            logger.info("Paciente encontrado por ID: {} (DNI: {}).", id, foundPaciente.getDni());
        } else {
            logger.info("Paciente no encontrado por ID: {}.", id);
        }
        return paciente;
    }
}