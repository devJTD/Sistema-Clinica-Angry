package com.clinica.sistema.Servicio;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clinica.sistema.Modelo.Cita;
import com.clinica.sistema.Modelo.Especialidad;
import com.clinica.sistema.Modelo.Horario;
import com.clinica.sistema.Modelo.Medico;
import com.clinica.sistema.Modelo.Notificacion;
import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Repositorio.CitaRepositorio;
import com.clinica.sistema.Repositorio.EspecialidadRepositorio;
import com.clinica.sistema.Repositorio.HorarioRepositorio;
import com.clinica.sistema.Repositorio.MedicoRepositorio;
import com.clinica.sistema.Repositorio.PacienteRepositorio;

@Service
public class CitaServicio {

    private final Logger logger = LoggerFactory.getLogger(CitaServicio.class);

    private final CitaRepositorio citaRepositorio;
    private final PacienteRepositorio pacienteRepositorio;
    private final MedicoRepositorio medicoRepositorio;
    private final HorarioRepositorio horarioRepositorio;
    private final EspecialidadRepositorio especialidadRepositorio;
    private final NotificacionServicio notificacionServicio;

    public CitaServicio(CitaRepositorio citaRepositorio, PacienteRepositorio pacienteRepositorio,
            MedicoRepositorio medicoRepositorio, HorarioRepositorio horarioRepositorio,
            EspecialidadRepositorio especialidadRepositorio, NotificacionServicio notificacionServicio) {
        this.citaRepositorio = citaRepositorio;
        this.pacienteRepositorio = pacienteRepositorio;
        this.medicoRepositorio = medicoRepositorio;
        this.horarioRepositorio = horarioRepositorio;
        this.especialidadRepositorio = especialidadRepositorio;
        this.notificacionServicio = notificacionServicio;
    }

    // Crea una nueva cita en el sistema.
    @Transactional
    public Cita crearCita(String fechaStr, String horaStr, Long idMedico, Long idPaciente) {
        logger.info("El paciente con ID {} esta intentando crear una cita para el medico con ID {} en la fecha {} a las {}.", idPaciente, idMedico, fechaStr, horaStr);

        // Valida que los parámetros de fecha, hora, ID de médico e ID de paciente no sean nulos o vacíos.
        if (fechaStr == null || fechaStr.isBlank()) {
            logger.warn("Validacion fallida: La fecha de la cita no puede estar vacia. Paciente ID: {}", idPaciente);
            throw new IllegalArgumentException("La fecha de la cita no puede estar vacia.");
        }
        if (horaStr == null || horaStr.isBlank()) {
            logger.warn("Validacion fallida: La hora de la cita no puede estar vacia. Paciente ID: {}", idPaciente);
            throw new IllegalArgumentException("La hora de la cita no puede estar vacia.");
        }
        if (idMedico == null || idMedico <= 0) {
            logger.warn("Validacion fallida: El ID del medico no puede ser nulo o negativo. Paciente ID: {}", idPaciente);
            throw new IllegalArgumentException("El ID del medico no puede ser nulo o negativo.");
        }
        if (idPaciente == null || idPaciente <= 0) {
            logger.warn("Validacion fallida: El ID del paciente no puede ser nulo o negativo. ID recibido: {}", idPaciente);
            throw new IllegalArgumentException("El ID del paciente no puede ser nulo o negativo.");
        }

        // Parsea las cadenas de fecha y hora a objetos LocalDate y LocalTime.
        LocalDate fechaCita = LocalDate.parse(fechaStr);
        LocalTime horaCita = LocalTime.parse(horaStr);
        logger.debug("Fecha parseada: {}, Hora parseada: {}.", fechaCita, horaCita);

        // Obtiene el paciente por su ID, lanzando una excepción si no se encuentra.
        Paciente paciente = pacienteRepositorio.findById(idPaciente)
                .orElseThrow(() -> {
                    logger.error("Error al crear cita: Paciente con ID {} no encontrado.", idPaciente);
                    return new IllegalArgumentException("Paciente con ID " + idPaciente + " no encontrado.");
                });
        logger.debug("Paciente '{} {}' (ID: {}) encontrado para la creacion de cita.", paciente.getNombre(), paciente.getApellido(), paciente.getId());

        // Obtiene el médico por su ID, lanzando una excepción si no se encuentra.
        Medico medico = medicoRepositorio.findById(idMedico)
                .orElseThrow(() -> {
                    logger.error("Error al crear cita para paciente ID {}: Medico con ID {} no encontrado.", idPaciente, idMedico);
                    return new IllegalArgumentException("Medico con ID " + idMedico + " no encontrado.");
                });
        logger.debug("Medico 'Dr. {} {}' (ID: {}) encontrado para la creacion de cita.", medico.getNombre(), medico.getApellido(), medico.getId());

        // Busca el horario específico para la fecha, hora y médico.
        Optional<Horario> horarioOptional = horarioRepositorio.findByFechaAndHoraAndMedico(fechaCita, horaCita, medico);
        // Lanza una excepción si el horario no está disponible.
        Horario horario = horarioOptional.orElseThrow(() -> {
            logger.error("Error al crear cita para paciente ID {} y medico ID {}: Horario disponible no encontrado para fecha {} y hora {}.", idPaciente, idMedico, fechaCita, horaCita);
            return new IllegalArgumentException(
                    "Horario disponible no encontrado para la fecha y hora especificadas.");
        });
        logger.debug("Horario ID {} encontrado. Disponibilidad actual: {}.", horario.getId(), horario.isDisponible());

        // Lanza una excepción si el horario no está disponible.
        if (!horario.isDisponible()) {
            logger.warn("El horario seleccionado (ID: {}) ya no esta disponible para la cita del paciente ID {}.", horario.getId(), idPaciente);
            throw new IllegalStateException("El horario seleccionado ya no esta disponible.");
        }

        // Crea una nueva instancia de Cita y establece sus propiedades.
        Cita nuevaCita = new Cita();
        nuevaCita.setFecha(fechaCita);
        nuevaCita.setHora(horaCita);
        nuevaCita.setEstado("Pendiente");
        nuevaCita.setPaciente(paciente);
        nuevaCita.setMedico(medico);
        logger.debug("Cita inicial construida: Fecha {}, Hora {}, Paciente ID {}, Medico ID {}. Estado: 'Pendiente'.", fechaCita, horaCita, idPaciente, idMedico);

        // Marca el horario como no disponible y lo guarda.
        horario.setDisponible(false);
        horarioRepositorio.save(horario);
        logger.info("Horario ID {} marcado como NO DISPONIBLE para medico {} en fecha {} a las {}.", horario.getId(), medico.getNombre() + " " + medico.getApellido(), horario.getFecha(), horario.getHora());

        // Guarda la nueva cita en el repositorio.
        Cita citaGuardada = citaRepositorio.save(nuevaCita);
        logger.info("Cita ID {} creada y guardada exitosamente para paciente {} con medico {} en estado 'Pendiente'.", citaGuardada.getId(), paciente.getNombre() + " " + paciente.getApellido(), medico.getNombre() + " " + medico.getApellido());

        try {
            // Construye el mensaje de confirmación de la cita.
            String mensajeContenido = String.format(
                    """
                            Hola %s,

                            Te confirmamos tu cita medica:
                            Medico: Dr. %s (%s)
                            Fecha: %s
                            Hora: %s

                            Por favor, se puntual. ¡Te esperamos!

                            Saludos cordiales,
                            Clinica Angry
                            """,
                    paciente.getNombre() + " " + paciente.getApellido(),
                    medico.getNombre() + " " + medico.getApellido(),
                    medico.getEspecialidad().getNombre(),
                    horario.getFecha().format(
                            DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    horario.getHora()
                            .format(DateTimeFormatter.ofPattern("HH:mm")));

            // Crea y configura la notificación para la cita.
            Notificacion nuevaNotificacion = new Notificacion();
            nuevaNotificacion.setMensaje(mensajeContenido);
            nuevaNotificacion.setEmailDestinatario(paciente.getCorreo());
            nuevaNotificacion.setFechaEnvio(LocalDate.now());

            // Asocia la notificación con la cita y guarda la cita actualizada.
            citaGuardada.setNotificacion(nuevaNotificacion);
            nuevaNotificacion.setCita(citaGuardada);

            citaRepositorio.save(citaGuardada);

            // Envía el correo de confirmación.
            notificacionServicio.enviarCorreoSimple(
                    paciente.getCorreo(),
                    "Confirmacion de Cita Medica - Clinica Angry",
                    mensajeContenido);
        } catch (Exception e) {
            logger.error("Error al enviar notificacion de confirmacion para la cita ID {} del paciente {}: {}. La cita ya fue guardada.", citaGuardada.getId(), paciente.getNombre() + " " + paciente.getApellido(), e.getMessage(), e);
        }
        return citaGuardada;
    }

    /**
     * Obtiene todas las citas de un paciente.
     * Es útil para operaciones donde se necesita procesar la totalidad de las citas del usuario,
     * como la actualización de estados basada en la fecha/hora.
     * @param idPaciente El ID del paciente.
     * @return Una lista de todas las citas asociadas al paciente.
     * @throws IllegalArgumentException si el paciente no es encontrado.
     */
    public List<Cita> obtenerTodasLasCitasPorPaciente(Long idPaciente) {
        logger.info("Se solicitan TODAS las citas para el paciente con ID: {}.", idPaciente);
        Paciente paciente = pacienteRepositorio.findById(idPaciente)
                .orElseThrow(() -> {
                    logger.error("Error al obtener todas las citas: Paciente con ID {} no encontrado.", idPaciente);
                    return new IllegalArgumentException("Paciente con ID " + idPaciente + " no encontrado.");
                });
        List<Cita> citas = citaRepositorio.findByPaciente(paciente);
        logger.info("Se recuperaron {} citas del paciente '{} {}' (ID: {}).", citas.size(), paciente.getNombre(), paciente.getApellido(), idPaciente);
        return citas;
    }

    // Obtiene las citas pendientes para un paciente específico.
    public List<Cita> obtenerCitasPendientesPorPaciente(Long idPaciente) {
        logger.info("Se solicitan citas pendientes para el paciente con ID: {}.", idPaciente);
        // Obtiene el paciente por su ID.
        Paciente paciente = pacienteRepositorio.findById(idPaciente)
                .orElseThrow(() -> {
                    logger.error("Error al obtener citas pendientes: Paciente con ID {} no encontrado.", idPaciente);
                    return new IllegalArgumentException(
                            "Paciente con ID " + idPaciente + " no encontrado.");
                });
        // Obtiene las citas del paciente con estado "Pendiente".
        List<Cita> citasPendientes = citaRepositorio.findByPacienteAndEstado(paciente, "Pendiente");
        logger.info("Se encontraron {} citas pendientes para el paciente '{} {}' (ID: {}).", citasPendientes.size(), paciente.getNombre(), paciente.getApellido(), idPaciente);
        return citasPendientes;
    }

    // Obtiene el historial de citas de un paciente.
    public List<Cita> obtenerHistorialCitasPorPaciente(Long idPaciente) {
        logger.info("Se solicita el historial de citas para el paciente con ID: {}.", idPaciente);
        // Obtiene el paciente por su ID.
        Paciente paciente = pacienteRepositorio.findById(idPaciente)
                .orElseThrow(() -> {
                    logger.error("Error al obtener historial de citas: Paciente con ID {} no encontrado.", idPaciente);
                    return new IllegalArgumentException(
                            "Paciente con ID " + idPaciente + " no encontrado.");
                });
        // Obtiene las citas del paciente cuyo estado no es "Pendiente".
        List<Cita> historialCitas = citaRepositorio.findByPacienteAndEstadoNot(paciente, "Pendiente");
        logger.info("Se encontraron {} citas en el historial para el paciente '{} {}' (ID: {}).", historialCitas.size(), paciente.getNombre(), paciente.getApellido(), idPaciente);
        return historialCitas;
    }

    // Cancela una cita específica.
    @Transactional
    public boolean cancelarCita(Long idCita) {
        logger.info("Se solicito la cancelacion de la cita con ID: {}.", idCita);
        // Busca la cita por su ID.
        Optional<Cita> citaOptional = citaRepositorio.findById(idCita);

        if (citaOptional.isPresent()) {
            Cita cita = citaOptional.get();
            String nombrePaciente = cita.getPaciente() != null ? cita.getPaciente().getNombre() + " " + cita.getPaciente().getApellido() : "Desconocido";
            String nombreMedico = cita.getMedico() != null ? cita.getMedico().getNombre() + " " + cita.getMedico().getApellido() : "Desconocido";

            logger.info("Cita encontrada (ID: {}) para el paciente '{}' y medico '{}'. Estado actual: '{}'.", idCita, nombrePaciente, nombreMedico, cita.getEstado());

            // Cambia el estado de la cita a "Cancelada".
            cita.setEstado("Cancelada");
            citaRepositorio.save(cita);
            logger.info("Cita ID {} del paciente '{}' y medico '{}' ha sido CANCELADA.", idCita, nombrePaciente, nombreMedico);

            // Busca el horario asociado a la cita y lo marca como disponible.
            Optional<Horario> horarioOptional = horarioRepositorio.findByFechaAndHoraAndMedico(
                    cita.getFecha(),
                    cita.getHora(), cita.getMedico());
            if (horarioOptional.isPresent()) {
                Horario horario = horarioOptional.get();
                horario.setDisponible(true);
                horarioRepositorio.save(horario);
                logger.info("Horario ID {} (Fecha: {}, Hora: {}) para medico '{}' marcado como DISPONIBLE tras la cancelacion de la cita.", horario.getId(), horario.getFecha(), horario.getHora(), nombreMedico);
            } else {
                logger.warn("No se encontro el horario correspondiente para la cita ID {} (Paciente: {}, Medico: {}) al intentar liberar la disponibilidad. Posible inconsistencia de datos.", idCita, nombrePaciente, nombreMedico);
            }
            return true;
        } else {
            logger.warn("Intento fallido de cancelar cita: No se encontro ninguna cita con ID {}.", idCita);
            return false;
        }
    }

    @Transactional
    public void guardarCitas(List<Cita> citas) {
        citaRepositorio.saveAll(citas);
        logger.debug("Se guardaron {} citas en la base de datos.", citas.size());
    }

    // Obtiene todas las especialidades disponibles.
    public List<Especialidad> obtenerTodasLasEspecialidades() {
        logger.info("Se solicito la obtencion de todas las especialidades.");
        // Obtiene todas las especialidades del repositorio.
        List<Especialidad> especialidades = especialidadRepositorio.findAll();
        logger.info("Se recuperaron {} especialidades de la base de datos.", especialidades.size());
        return especialidades;
    }

    // Obtiene médicos por una especialidad específica.
    public List<Medico> obtenerMedicosPorEspecialidad(Long idEspecialidad) {
        logger.info("Se solicitan medicos para la especialidad con ID: {}.", idEspecialidad);
        // Valida que el ID de la especialidad no sea nulo o negativo.
        if (idEspecialidad == null || idEspecialidad <= 0) {
            logger.warn("Validacion fallida: El ID de la especialidad no puede ser nulo o negativo. ID recibido: {}", idEspecialidad);
            throw new IllegalArgumentException("El ID de la especialidad no puede ser nulo o negativo.");
        }
        // Obtiene la especialidad por su ID.
        Especialidad especialidad = especialidadRepositorio.findById(idEspecialidad)
                .orElseThrow(() -> {
                    logger.error("Error al obtener medicos: Especialidad con ID {} no encontrada.", idEspecialidad);
                    return new IllegalArgumentException(
                            "Especialidad con ID " + idEspecialidad + " no encontrada.");
                });
        logger.debug("Especialidad '{}' (ID: {}) encontrada.", especialidad.getNombre(), idEspecialidad);
        // Obtiene los médicos asociados a la especialidad.
        List<Medico> medicos = medicoRepositorio.findByEspecialidad(especialidad);
        logger.info("Se encontraron {} medicos para la especialidad '{}'.", medicos.size(), especialidad.getNombre());
        return medicos;
    }

    // Obtiene los horarios disponibles para un médico en una fecha determinada.
    public List<Horario> obtenerHorariosDisponiblesPorMedicoYFecha(Long idMedico, LocalDate fecha) {
        logger.info("Se solicitan horarios disponibles para el medico con ID: {} en la fecha: {}.", idMedico, fecha);
        // Valida que el ID del médico y la fecha no sean nulos o inválidos.
        if (idMedico == null || idMedico <= 0) {
            logger.warn("Validacion fallida: El ID del medico no puede ser nulo o negativo. ID recibido: {}", idMedico);
            throw new IllegalArgumentException("El ID del medico no puede ser nulo o negativo.");
        }
        if (fecha == null) {
            logger.warn("Validacion fallida: La fecha no puede ser nula para buscar horarios del medico ID {}.", idMedico);
            throw new IllegalArgumentException("La fecha no puede ser nula.");
        }
        // Obtiene el médico por su ID.
        Medico medico = medicoRepositorio.findById(idMedico)
                .orElseThrow(() -> {
                    logger.error("Error al obtener horarios: Medico con ID {} no encontrado.", idMedico);
                    return new IllegalArgumentException("Medico con ID " + idMedico + " no encontrado.");
                });
        logger.debug("Medico 'Dr. {} {}' (ID: {}) encontrado para buscar horarios.", medico.getNombre(), medico.getApellido(), idMedico);
        // Obtiene los horarios disponibles para el médico y la fecha.
        List<Horario> horarios = horarioRepositorio.findByMedicoAndFechaAndDisponibleTrue(medico, fecha);
        logger.info("Se encontraron {} horarios disponibles para el medico '{}' en la fecha {}.", horarios.size(), medico.getNombre() + " " + medico.getApellido(), fecha);
        return horarios;
    }
}