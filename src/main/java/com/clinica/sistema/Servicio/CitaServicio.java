package com.clinica.sistema.Servicio;

import com.clinica.sistema.Modelo.Cita;
import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Modelo.Medico;
import com.clinica.sistema.Modelo.Notificacion;
import com.clinica.sistema.Modelo.Horario;
import com.clinica.sistema.Modelo.Especialidad;

import com.clinica.sistema.Repositorio.CitaRepositorio;
import com.clinica.sistema.Repositorio.PacienteRepositorio;
import com.clinica.sistema.Repositorio.MedicoRepositorio;
import com.clinica.sistema.Repositorio.HorarioRepositorio;
import com.clinica.sistema.Repositorio.EspecialidadRepositorio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class CitaServicio {

    private final CitaRepositorio citaRepositorio;
    private final PacienteRepositorio pacienteRepositorio;
    private final MedicoRepositorio medicoRepositorio;
    private final HorarioRepositorio horarioRepositorio;
    private final EspecialidadRepositorio especialidadRepositorio;
    private final NotificacionServicio notificacionServicio; // Inyectar NotificacionServicio

    private final Logger logger = LoggerFactory.getLogger(CitaServicio.class);

    public CitaServicio(CitaRepositorio citaRepositorio, PacienteRepositorio pacienteRepositorio,
            MedicoRepositorio medicoRepositorio, HorarioRepositorio horarioRepositorio,
            EspecialidadRepositorio especialidadRepositorio, NotificacionServicio notificacionServicio) {
        this.citaRepositorio = citaRepositorio;
        this.pacienteRepositorio = pacienteRepositorio;
        this.medicoRepositorio = medicoRepositorio;
        this.horarioRepositorio = horarioRepositorio;
        this.especialidadRepositorio = especialidadRepositorio;
        this.notificacionServicio = notificacionServicio;
        logger.info("[CitaServicio] - Servicio inicializado con dependencias inyectadas.");
    }

    @Transactional
    public Cita crearCita(String fechaStr, String horaStr, Long idMedico, Long idPaciente) {
        logger.info(
                "[CitaServicio] - INICIO: Solicitud para crear cita con fecha: {}, hora: {}, idMedico: {}, idPaciente: {}",
                fechaStr, horaStr, idMedico, idPaciente);

        // --- Validaciones iniciales ---
        if (fechaStr == null || fechaStr.isBlank()) {
            logger.warn("[CitaServicio] - Validación fallida: La fecha de la cita (fechaStr) está vacía.");
            throw new IllegalArgumentException("La fecha de la cita no puede estar vacía.");
        }
        if (horaStr == null || horaStr.isBlank()) {
            logger.warn("[CitaServicio] - Validación fallida: La hora de la cita (horaStr) está vacía.");
            throw new IllegalArgumentException("La hora de la cita no puede estar vacía.");
        }
        if (idMedico == null || idMedico <= 0) {
            logger.warn("[CitaServicio] - Validación fallida: El ID del médico es nulo o inválido ({}).", idMedico);
            throw new IllegalArgumentException("El ID del médico no puede ser nulo o negativo.");
        }
        if (idPaciente == null || idPaciente <= 0) {
            logger.warn("[CitaServicio] - Validación fallida: El ID del paciente es nulo o inválido ({}).", idPaciente);
            throw new IllegalArgumentException("El ID del paciente no puede ser nulo o negativo.");
        }

        LocalDate fechaCita = LocalDate.parse(fechaStr);
        LocalTime horaCita = LocalTime.parse(horaStr);
        logger.debug("[CitaServicio] - Datos parseados: fechaCita='{}', horaCita='{}'", fechaCita, horaCita);

        // --- Obtener entidades relacionadas ---
        logger.info("[CitaServicio] - Buscando Paciente con ID: {}", idPaciente);
        Paciente paciente = pacienteRepositorio.findById(idPaciente)
                .orElseThrow(() -> {
                    logger.error("[CitaServicio] - Error: Paciente con ID {} no encontrado.", idPaciente);
                    return new IllegalArgumentException("Paciente con ID " + idPaciente + " no encontrado.");
                });
        logger.info("[CitaServicio] - Paciente encontrado: ID {}, Nombre: {} {}, Correo: {}", paciente.getId(),
                paciente.getNombre(), paciente.getApellido(), paciente.getCorreo());

        logger.info("[CitaServicio] - Buscando Médico con ID: {}", idMedico);
        Medico medico = medicoRepositorio.findById(idMedico)
                .orElseThrow(() -> {
                    logger.error("[CitaServicio] - Error: Médico con ID {} no encontrado.", idMedico);
                    return new IllegalArgumentException("Médico con ID " + idMedico + " no encontrado.");
                });
        logger.info("[CitaServicio] - Médico encontrado: ID {}, Nombre: {}, Especialidad: {}", medico.getId(),
                medico.getNombre(), medico.getEspecialidad());

        logger.info("[CitaServicio] - Buscando Horario disponible para fecha: {}, hora: {}, y médico ID: {}", fechaCita,
                horaCita, idMedico);
        Optional<Horario> horarioOptional = horarioRepositorio.findByFechaAndHoraAndMedico(fechaCita, horaCita, medico);
        Horario horario = horarioOptional.orElseThrow(() -> {
            logger.error(
                    "[CitaServicio] - Error: Horario disponible no encontrado para fecha: {}, hora: {}, y médico ID: {}.",
                    fechaCita, horaCita, idMedico);
            return new IllegalArgumentException("Horario disponible no encontrado para la fecha y hora especificadas.");
        });
        logger.info("[CitaServicio] - Horario encontrado: ID {}, Fecha: {}, Hora: {}, Disponible: {}", horario.getId(),
                horario.getFecha(), horario.getHora(), horario.isDisponible());

        if (!horario.isDisponible()) {
            logger.error("[CitaServicio] - El horario ID {} ya no está disponible. No se puede crear la cita.",
                    horario.getId());
            throw new IllegalStateException("El horario seleccionado ya no está disponible.");
        }

        // --- Crear la nueva Cita ---
        logger.info("[CitaServicio] - Creando nueva instancia de Cita.");
        Cita nuevaCita = new Cita();
        nuevaCita.setFecha(fechaCita);
        nuevaCita.setHora(horaCita);
        nuevaCita.setEstado("Pendiente");
        nuevaCita.setPaciente(paciente);
        nuevaCita.setMedico(medico);
        logger.debug(
                "[CitaServicio] - Detalles de nueva cita antes de guardar: Fecha='{}', Hora='{}', Estado='{}', Paciente ID={}, Medico ID={}",
                nuevaCita.getFecha(), nuevaCita.getHora(), nuevaCita.getEstado(), nuevaCita.getPaciente().getId(),
                nuevaCita.getMedico().getId());

        // --- Actualizar disponibilidad del horario ---
        logger.info("[CitaServicio] - Marcando horario ID {} como no disponible.", horario.getId());
        horario.setDisponible(false);
        horarioRepositorio.save(horario);
        logger.info("[CitaServicio] - Horario ID {} guardado con disponibilidad: {}. ", horario.getId(),
                horario.isDisponible());

        // --- Guardar la Cita inicialmente ---
        logger.info("[CitaServicio] - Guardando la Cita inicial en la base de datos.");
        Cita citaGuardada = citaRepositorio.save(nuevaCita);
        logger.info("[CitaServicio] - Cita guardada exitosamente con ID: {}.", citaGuardada.getId());

        // --- LÓGICA PARA CREAR Y ASOCIAR LA NOTIFICACIÓN Y ENVIAR EL CORREO ---
        logger.info("[CitaServicio] - INICIO LÓGICA DE NOTIFICACIÓN para Cita ID: {}.", citaGuardada.getId());
        try {
            // 1. Construir el mensaje de la notificación
            logger.debug("[CitaServicio] - Construyendo el mensaje de notificación para el paciente: {}.",
                    paciente.getCorreo());
            String mensajeContenido = String.format(
                    "Hola %s,\n\n" +
                            "Te confirmamos tu cita médica:\n" +
                            "Médico: Dr. %s (%s)\n" + // <--- Esta es la línea que cambiaremos
                            "Fecha: %s\n" +
                            "Hora: %s\n\n" +
                            "Por favor, sé puntual. ¡Te esperamos!\n\n" +
                            "Saludos cordiales,\n" +
                            "Clínica Angry",
                    paciente.getNombre() + " " + paciente.getApellido(), // Usar nombre completo
                    medico.getNombre() + " " + medico.getApellido(),
                    medico.getEspecialidad().getNombre(), // Este es el nombre de la especialidad
                    horario.getFecha().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    horario.getHora().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            logger.debug("[CitaServicio] - Mensaje de notificación generado:\n{}", mensajeContenido);

            // 2. Crear una nueva instancia de Notificacion
            logger.info("[CitaServicio] - Creando nueva instancia de Notificacion.");
            Notificacion nuevaNotificacion = new Notificacion();
            nuevaNotificacion.setMensaje(mensajeContenido);
            nuevaNotificacion.setEmailDestinatario(paciente.getCorreo());
            nuevaNotificacion.setFechaEnvio(LocalDate.now()); // Mantenido como LocalDate.now() según tu indicación
            logger.debug("[CitaServicio] - Detalles de Notificacion antes de asociar: Destinatario={}, FechaEnvio={}",
                    nuevaNotificacion.getEmailDestinatario(), nuevaNotificacion.getFechaEnvio());

            // 3. Asociar la Notificacion a la Cita
            logger.info("[CitaServicio] - Asociando Notificacion a la Cita ID: {}.", citaGuardada.getId());
            citaGuardada.setNotificacion(nuevaNotificacion);
            nuevaNotificacion.setCita(citaGuardada); // Establecer la relación inversa también

            // Volver a guardar la cita para que se persista la relación con Notificacion
            logger.info("[CitaServicio] - Persistiendo la Cita (con la Notificacion asociada) en la base de datos.");
            citaRepositorio.save(citaGuardada);
            logger.info(
                    "[CitaServicio] - Notificación creada y asociada exitosamente a la Cita ID: {}. Notificacion ID: {}.",
                    citaGuardada.getId(), nuevaNotificacion.getId());

            // 4. Usar el NotificacionServicio para enviar el correo
            logger.info("[CitaServicio] - Llamando a NotificacionServicio para enviar correo a: {}. Asunto: '{}'",
                    paciente.getCorreo(), "Confirmación de Cita Médica - Clínica Angry");
            notificacionServicio.enviarCorreoSimple(
                    paciente.getCorreo(), // Destinatario: Correo del paciente
                    "Confirmación de Cita Médica - Clínica Angry", // Asunto del correo
                    mensajeContenido // Contenido del mensaje
            );
            logger.info(
                    "[CitaServicio] - El método enviarCorreoSimple de NotificacionServicio ha sido invocado para el correo: {}.",
                    paciente.getCorreo());

        } catch (Exception e) {
            logger.error(
                    "[CitaServicio] - FATAL: Error inesperado al generar notificación o enviar correo para Cita ID {}: {}. Stack Trace: ",
                    citaGuardada.getId(), e.getMessage(), e);
            // La cita y la notificación ya están guardadas. Aquí se captura el error de
            // envío de correo.
        }
        logger.info("[CitaServicio] - FIN LÓGICA DE NOTIFICACIÓN para Cita ID: {}.", citaGuardada.getId());

        logger.info("[CitaServicio] - FIN: Método crearCita. Retornando Cita ID: {}.", citaGuardada.getId());
        return citaGuardada;
    }

    public List<Cita> obtenerTodasLasCitas() {
        logger.info("[CitaServicio] - Obtener todas las citas solicitado.");
        return citaRepositorio.findAll();
    }

    public List<Cita> obtenerCitasPendientesPorPaciente(Long idPaciente) {
        logger.info("[CitaServicio] - Buscando citas pendientes para Paciente ID: {}.", idPaciente);
        Paciente paciente = pacienteRepositorio.findById(idPaciente)
                .orElseThrow(() -> new IllegalArgumentException("Paciente con ID " + idPaciente + " no encontrado."));
        logger.debug("[CitaServicio] - Paciente encontrado para citas pendientes: ID {}", paciente.getId());
        return citaRepositorio.findByPacienteAndEstado(paciente, "Pendiente");
    }

    public List<Cita> obtenerHistorialCitasPorPaciente(Long idPaciente) {
        logger.info("[CitaServicio] - Buscando historial de citas para Paciente ID: {}.", idPaciente);
        Paciente paciente = pacienteRepositorio.findById(idPaciente)
                .orElseThrow(() -> new IllegalArgumentException("Paciente con ID " + idPaciente + " no encontrado."));
        logger.debug("[CitaServicio] - Paciente encontrado para historial de citas: ID {}", paciente.getId());
        return citaRepositorio.findByPacienteAndEstadoNot(paciente, "Pendiente");
    }

    @Transactional
    public boolean cancelarCita(Long idCita) {
        logger.info("[CitaServicio] - Solicitud de cancelación de cita para Cita ID: {}.", idCita);
        Optional<Cita> citaOptional = citaRepositorio.findById(idCita);

        if (citaOptional.isPresent()) {
            Cita cita = citaOptional.get();
            logger.info("[CitaServicio] - Cita ID {} encontrada. Cambiando estado a 'Cancelada'.", idCita);
            cita.setEstado("Cancelada");
            citaRepositorio.save(cita);
            logger.info("[CitaServicio] - Cita ID {} guardada con estado 'Cancelada'.", idCita);

            logger.info("[CitaServicio] - Buscando horario asociado a Cita ID {} para marcarlo como disponible.",
                    idCita);
            Optional<Horario> horarioOptional = horarioRepositorio.findByFechaAndHoraAndMedico(cita.getFecha(),
                    cita.getHora(), cita.getMedico());
            if (horarioOptional.isPresent()) {
                Horario horario = horarioOptional.get();
                logger.debug("[CitaServicio] - Horario ID {} encontrado para re-disponibilidad.", horario.getId());
                horario.setDisponible(true);
                horarioRepositorio.save(horario);
                logger.info(
                        "[CitaServicio] - Horario {}-{}-{} del médico {} marcado como disponible tras cancelación de cita {}.",
                        horario.getFecha(), horario.getHora(), horario.getMedico().getId(), idCita);
            } else {
                logger.warn(
                        "[CitaServicio] - Advertencia: No se encontró el horario asociado a la cita ID {} para re-marcarlo como disponible.",
                        idCita);
            }
            logger.info("[CitaServicio] - Cita ID {} cancelada exitosamente.", idCita);
            return true;
        } else {
            logger.warn("[CitaServicio] - Intento de cancelar cita con ID {} fallido: Cita no encontrada.", idCita);
            return false;
        }
    }

    public List<Especialidad> obtenerTodasLasEspecialidades() {
        logger.info("[CitaServicio] - Cargando todas las especialidades.");
        return especialidadRepositorio.findAll();
    }

    public List<Medico> obtenerMedicosPorEspecialidad(Long idEspecialidad) {
        logger.info("[CitaServicio] - Buscando médicos para la especialidad ID: {}.", idEspecialidad);
        if (idEspecialidad == null || idEspecialidad <= 0) {
            logger.warn("[CitaServicio] - Validación fallida: El ID de la especialidad es nulo o negativo ({}).",
                    idEspecialidad);
            throw new IllegalArgumentException("El ID de la especialidad no puede ser nulo o negativo.");
        }
        Especialidad especialidad = especialidadRepositorio.findById(idEspecialidad)
                .orElseThrow(() -> {
                    logger.error("[CitaServicio] - Error: Especialidad con ID {} no encontrada.", idEspecialidad);
                    return new IllegalArgumentException("Especialidad con ID " + idEspecialidad + " no encontrada.");
                });
        logger.debug("[CitaServicio] - Especialidad encontrada: ID {}, Nombre: {}.", especialidad.getId(),
                especialidad.getNombre());
        return medicoRepositorio.findByEspecialidad(especialidad);
    }

    public List<Horario> obtenerHorariosDisponiblesPorMedicoYFecha(Long idMedico, LocalDate fecha) {
        logger.info("[CitaServicio] - Buscando horarios disponibles para Médico ID: {} y Fecha: {}.", idMedico, fecha);
        Medico medico = medicoRepositorio.findById(idMedico)
                .orElseThrow(() -> {
                    logger.error("[CitaServicio] - Error: Médico con ID {} no encontrado.", idMedico);
                    return new IllegalArgumentException("Médico con ID " + idMedico + " no encontrado.");
                });
        logger.debug("[CitaServicio] - Médico encontrado para horarios disponibles: ID {}", medico.getId());
        return horarioRepositorio.findByMedicoAndFechaAndDisponibleTrue(medico, fecha);
    }
}