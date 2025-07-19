package com.clinica.sistema.Servicio;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clinica.sistema.Modelo.Direccion;
import com.clinica.sistema.Repositorio.DireccionRepositorio;
import com.clinica.sistema.Repositorio.PacienteRepositorio;

@Service
public class DireccionServicio {

    private final Logger logger = LoggerFactory.getLogger(DireccionServicio.class);

    private final DireccionRepositorio direccionRepositorio;
    private final PacienteRepositorio pacienteRepositorio; // Inyectar PacienteRepositorio

    // Constructor que inyecta los repositorios necesarios.
    public DireccionServicio(DireccionRepositorio direccionRepositorio, PacienteRepositorio pacienteRepositorio) {
        this.direccionRepositorio = direccionRepositorio;
        this.pacienteRepositorio = pacienteRepositorio; // Inicializar PacienteRepositorio
    }

    // Metodo auxiliar para obtener la informacion del paciente para el log
    private String getPacienteLogInfo(Long pacienteId) {
        return pacienteRepositorio.findById(pacienteId)
            .map(p -> String.format("Paciente %s %s (ID: %d, DNI: %s)", p.getNombre(), p.getApellido(), p.getId(), p.getDni()))
            .orElseGet(() -> String.format("Paciente con ID %d (no encontrado)", pacienteId));
    }

    // Obtiene una lista de direcciones asociadas a un ID de paciente especifico.
    public List<Direccion> obtenerDireccionesPorPaciente(Long pacienteId) {
        // Valida que el pacienteId no sea nulo o invalido.
        if (pacienteId == null || pacienteId <= 0) {
            logger.warn("Validacion fallida: El ID del paciente no puede ser nulo o negativo al intentar obtener direcciones.");
            throw new IllegalArgumentException("El ID del paciente no puede ser nulo o negativo.");
        }

        // Obtenemos la informacion del paciente para usarla directamente en el log.
        String pacienteInfo = getPacienteLogInfo(pacienteId);
        logger.info("El {} esta solicitando sus direcciones.", pacienteInfo);

        // Busca las direcciones por el ID del paciente.
        List<Direccion> direcciones = direccionRepositorio.findByPacienteId(pacienteId);
        logger.info("El {} ha recuperado {} direcciones.", pacienteInfo, direcciones.size());
        return direcciones;
    }

    // Guarda una nueva direccion en la base de datos.
    @Transactional
    public Direccion guardarDireccion(Direccion direccion) {
        // Valida que el objeto Direccion no sea nulo.
        if (direccion == null) {
            logger.warn("Validacion fallida al intentar guardar una direccion: La direccion proporcionada es nula.");
            throw new IllegalArgumentException("La direccion a guardar no puede ser nula.");
        }
        // Valida que la direccion este asociada a un paciente valido.
        if (direccion.getPaciente() == null || direccion.getPaciente().getId() == null || direccion.getPaciente().getId() <= 0) {
            logger.warn("Validacion fallida al intentar guardar direccion: La direccion debe estar asociada a un paciente valido. Direccion Completa: {}", direccion.getDireccionCompleta());
            throw new IllegalArgumentException("La direccion debe estar asociada a un paciente valido.");
        }

        Long pacienteId = direccion.getPaciente().getId();
        // Obtenemos la informacion del paciente para usarla directamente en el log.
        String pacienteInfo = getPacienteLogInfo(pacienteId);

        logger.info("El {} esta guardando una nueva direccion (Direccion: {}).", pacienteInfo, direccion.getDireccionCompleta());
        // Guarda la direccion en el repositorio.
        Direccion direccionGuardada = direccionRepositorio.save(direccion);
        logger.info("El {} ha guardado la direccion ID {} exitosamente.", pacienteInfo, direccionGuardada.getId());
        return direccionGuardada;
    }

    // Busca una direccion por su ID y el ID del paciente al que pertenece.
    public Optional<Direccion> buscarPorIdYPacienteId(Long id, Long pacienteId) {
        // Valida que los IDs no sean nulos o invalidos.
        if (id == null || id <= 0) {
            logger.warn("Validacion fallida: El ID de la direccion no puede ser nulo o negativo al buscar por ID de paciente.");
            throw new IllegalArgumentException("El ID de la direccion no puede ser nulo o negativo.");
        }
        if (pacienteId == null || pacienteId <= 0) {
            logger.warn("Validacion fallida: El ID del paciente no puede ser nulo o negativo al buscar direccion por ID y paciente ID.");
            throw new IllegalArgumentException("El ID del paciente no puede ser nulo o negativo.");
        }

        // Obtenemos la informacion del paciente para usarla directamente en el log.
        String pacienteInfo = getPacienteLogInfo(pacienteId);
        logger.info("El {} esta buscando la direccion con ID: {}.", pacienteInfo, id);

        // Busca la direccion por ambos IDs.
        Optional<Direccion> direccion = direccionRepositorio.findByIdAndPacienteId(id, pacienteId);
        if (direccion.isPresent()) {
            logger.info("El {} ha encontrado la direccion ID {}.", pacienteInfo, id);
        } else {
            logger.info("El {} no encontro la direccion con ID {}.", pacienteInfo, id);
        }
        return direccion;
    }
}