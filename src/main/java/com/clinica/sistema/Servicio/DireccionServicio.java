package com.clinica.sistema.Servicio;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger; 
import org.slf4j.LoggerFactory; 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clinica.sistema.Modelo.Direccion;
import com.clinica.sistema.Repositorio.DireccionRepositorio;

@Service
public class DireccionServicio {

    private final Logger logger = LoggerFactory.getLogger(DireccionServicio.class); 

    private final DireccionRepositorio direccionRepositorio;

    public DireccionServicio(DireccionRepositorio direccionRepositorio) {
        this.direccionRepositorio = direccionRepositorio;
    }

    public List<Direccion> obtenerDireccionesPorPaciente(Long pacienteId) {
        // Validar que el pacienteId no sea nulo o invalido
        if (pacienteId == null || pacienteId <= 0) {
            logger.warn("Validacion fallida: El ID del paciente no puede ser nulo o negativo al intentar obtener direcciones. ID recibido: {}", pacienteId);
            throw new IllegalArgumentException("El ID del paciente no puede ser nulo o negativo.");
        }
        logger.info("Solicitando direcciones para el paciente con ID: {}", pacienteId);
        List<Direccion> direcciones = direccionRepositorio.findByPacienteId(pacienteId);
        logger.info("Se encontraron {} direcciones para el paciente con ID: {}.", direcciones.size(), pacienteId);
        return direcciones;
    }

    @Transactional
    public Direccion guardarDireccion(Direccion direccion) {
        // Validar que el objeto Direccion no sea nulo
        if (direccion == null) {
            logger.warn("Validacion fallida al intentar guardar una direccion: La direccion proporcionada es nula.");
            throw new IllegalArgumentException("La direccion a guardar no puede ser nula.");
        }
        // Validar que el paciente asociado no sea nulo y tenga ID
        if (direccion.getPaciente() == null || direccion.getPaciente().getId() == null || direccion.getPaciente().getId() <= 0) {
            logger.warn("Validacion fallida al intentar guardar direccion: La direccion debe estar asociada a un paciente valido. Direccion Completa: {}", direccion.getDireccionCompleta());
            throw new IllegalArgumentException("La direccion debe estar asociada a un paciente valido.");
        }

        logger.info("Guardando nueva direccion para el paciente con ID: {} (Direccion: {}).", direccion.getPaciente().getId(), direccion.getDireccionCompleta());
        Direccion direccionGuardada = direccionRepositorio.save(direccion);
        logger.info("Direccion ID {} guardada exitosamente para el paciente con ID {}.", direccionGuardada.getId(), direccionGuardada.getPaciente().getId());
        return direccionGuardada;
    }

    public Optional<Direccion> buscarPorIdYPacienteId(Long id, Long pacienteId) {
        // Validar que los IDs no sean nulos o invalidos
        if (id == null || id <= 0) {
            logger.warn("Validacion fallida: El ID de la direccion no puede ser nulo o negativo al buscar por ID de paciente. ID recibido: {}", id);
            throw new IllegalArgumentException("El ID de la direccion no puede ser nulo o negativo.");
        }
        if (pacienteId == null || pacienteId <= 0) {
            logger.warn("Validacion fallida: El ID del paciente no puede ser nulo o negativo al buscar direccion por ID y paciente ID. Paciente ID recibido: {}", pacienteId);
            throw new IllegalArgumentException("El ID del paciente no puede ser nulo o negativo.");
        }

        logger.info("Buscando direccion con ID: {} para el paciente con ID: {}.", id, pacienteId);
        Optional<Direccion> direccion = direccionRepositorio.findByIdAndPacienteId(id, pacienteId);
        if (direccion.isPresent()) {
            logger.info("Direccion ID {} encontrada para el paciente ID {}.", id, pacienteId);
        } else {
            logger.info("No se encontro la direccion con ID {} para el paciente ID {}.", id, pacienteId);
        }
        return direccion;
    }
}