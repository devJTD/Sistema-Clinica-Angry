package com.clinica.sistema.Servicio;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Importar la clase MDC

import com.clinica.sistema.Modelo.Direccion;
import com.clinica.sistema.Repositorio.DireccionRepositorio;
import com.clinica.sistema.Repositorio.PacienteRepositorio; // Necesitamos el repositorio de Paciente

@Service
public class DireccionServicio {

    private final Logger logger = LoggerFactory.getLogger(DireccionServicio.class);

    private final DireccionRepositorio direccionRepositorio;
    private final PacienteRepositorio pacienteRepositorio; // Inyectar PacienteRepositorio

    // Constantes para las claves MDC del usuario/paciente
    private static final String MDC_USER_FULL_NAME = "userFullName";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_USER_DNI = "userDni";

    public DireccionServicio(DireccionRepositorio direccionRepositorio, PacienteRepositorio pacienteRepositorio) {
        this.direccionRepositorio = direccionRepositorio;
        this.pacienteRepositorio = pacienteRepositorio; // Inicializar PacienteRepositorio
    }

    // Método auxiliar para establecer información del paciente en el MDC
    private void setPacienteMDCContext(Long idPaciente) {
        try {
            pacienteRepositorio.findById(idPaciente).ifPresent(paciente -> {
                MDC.put(MDC_USER_FULL_NAME, paciente.getNombre() + " " + paciente.getApellido());
                MDC.put(MDC_USER_ID, String.valueOf(paciente.getId()));
                MDC.put(MDC_USER_DNI, paciente.getDni());
            });
        } catch (Exception e) {
            logger.warn("No se pudo establecer el contexto MDC para el paciente con ID: {}. Error: {}", idPaciente, e.getMessage());
            clearPacienteMDCContext(); // Limpiar si hay un error al obtener el paciente
        }
    }

    // Método auxiliar para limpiar información del paciente del MDC
    private void clearPacienteMDCContext() {
        MDC.remove(MDC_USER_FULL_NAME);
        MDC.remove(MDC_USER_ID);
        MDC.remove(MDC_USER_DNI);
    }

    // Obtiene una lista de direcciones asociadas a un ID de paciente específico.
    public List<Direccion> obtenerDireccionesPorPaciente(Long pacienteId) {
        setPacienteMDCContext(pacienteId); // Establecer MDC al inicio del método
        try {
            // Valida que el pacienteId no sea nulo o inválido.
            if (pacienteId == null || pacienteId <= 0) {
                logger.warn("Validación fallida: El ID del paciente no puede ser nulo o negativo al intentar obtener direcciones.");
                throw new IllegalArgumentException("El ID del paciente no puede ser nulo o negativo.");
            }
            logger.info("El paciente {} (ID: {}, DNI: {}) está solicitando sus direcciones.",
                         MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI));
            // Busca las direcciones por el ID del paciente.
            List<Direccion> direcciones = direccionRepositorio.findByPacienteId(pacienteId);
            logger.info("El paciente {} (ID: {}, DNI: {}) ha recuperado {} direcciones.",
                         MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI), direcciones.size());
            return direcciones;
        } finally {
            clearPacienteMDCContext(); // Limpiar MDC al finalizar el método
        }
    }

    // Guarda una nueva dirección en la base de datos.
    @Transactional
    public Direccion guardarDireccion(Direccion direccion) {
        // Valida que el objeto Direccion no sea nulo.
        if (direccion == null) {
            logger.warn("Validación fallida al intentar guardar una dirección: La dirección proporcionada es nula.");
            throw new IllegalArgumentException("La dirección a guardar no puede ser nula.");
        }
        // Valida que la dirección esté asociada a un paciente válido.
        if (direccion.getPaciente() == null || direccion.getPaciente().getId() == null || direccion.getPaciente().getId() <= 0) {
            logger.warn("Validación fallida al intentar guardar dirección: La dirección debe estar asociada a un paciente válido. Dirección Completa: {}", direccion.getDireccionCompleta());
            throw new IllegalArgumentException("La dirección debe estar asociada a un paciente válido.");
        }

        setPacienteMDCContext(direccion.getPaciente().getId()); // Establecer MDC al inicio del método
        try {
            logger.info("El paciente {} (ID: {}, DNI: {}) está guardando una nueva dirección (Dirección: {}).",
                         MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI), direccion.getDireccionCompleta());
            // Guarda la dirección en el repositorio.
            Direccion direccionGuardada = direccionRepositorio.save(direccion);
            logger.info("El paciente {} (ID: {}, DNI: {}) ha guardado la dirección ID {} exitosamente.",
                         MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI), direccionGuardada.getId());
            return direccionGuardada;
        } finally {
            clearPacienteMDCContext(); // Limpiar MDC al finalizar el método
        }
    }

    // Busca una dirección por su ID y el ID del paciente al que pertenece.
    public Optional<Direccion> buscarPorIdYPacienteId(Long id, Long pacienteId) {
        setPacienteMDCContext(pacienteId); // Establecer MDC al inicio del método
        try {
            // Valida que los IDs no sean nulos o inválidos.
            if (id == null || id <= 0) {
                logger.warn("Validación fallida: El ID de la dirección no puede ser nulo o negativo al buscar por ID de paciente.");
                throw new IllegalArgumentException("El ID de la dirección no puede ser nulo o negativo.");
            }
            if (pacienteId == null || pacienteId <= 0) {
                logger.warn("Validación fallida: El ID del paciente no puede ser nulo o negativo al buscar dirección por ID y paciente ID.");
                throw new IllegalArgumentException("El ID del paciente no puede ser nulo o negativo.");
            }

            logger.info("El paciente {} (ID: {}, DNI: {}) está buscando la dirección con ID: {}.",
                         MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI), id);
            // Busca la dirección por ambos IDs.
            Optional<Direccion> direccion = direccionRepositorio.findByIdAndPacienteId(id, pacienteId);
            if (direccion.isPresent()) {
                logger.info("El paciente {} (ID: {}, DNI: {}) ha encontrado la dirección ID {}.",
                             MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI), id);
            } else {
                logger.info("El paciente {} (ID: {}, DNI: {}) no encontró la dirección con ID {}.",
                             MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI), id);
            }
            return direccion;
        } finally {
            clearPacienteMDCContext(); // Limpiar MDC al finalizar el método
        }
    }
}