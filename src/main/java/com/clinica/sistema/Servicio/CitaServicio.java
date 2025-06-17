package com.clinica.sistema.Servicio;

import com.clinica.sistema.Modelo.Cita;
import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Modelo.Medico;
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

    private final Logger logger = LoggerFactory.getLogger(CitaServicio.class);

    public CitaServicio(CitaRepositorio citaRepositorio, PacienteRepositorio pacienteRepositorio,
                        MedicoRepositorio medicoRepositorio, HorarioRepositorio horarioRepositorio,
                        EspecialidadRepositorio especialidadRepositorio) {
        this.citaRepositorio = citaRepositorio;
        this.pacienteRepositorio = pacienteRepositorio;
        this.medicoRepositorio = medicoRepositorio;
        this.horarioRepositorio = horarioRepositorio;
        this.especialidadRepositorio = especialidadRepositorio;
    }

    @Transactional
    public Cita crearCita(String fechaStr, String horaStr, Long idMedico, Long idPaciente) {
        if (fechaStr == null || fechaStr.isBlank()) {
            throw new IllegalArgumentException("La fecha de la cita no puede estar vacía.");
        }
        if (horaStr == null || horaStr.isBlank()) {
            throw new IllegalArgumentException("La hora de la cita no puede estar vacía.");
        }
        if (idMedico == null || idMedico <= 0) {
            throw new IllegalArgumentException("El ID del médico no puede ser nulo o negativo.");
        }
        if (idPaciente == null || idPaciente <= 0) {
            throw new IllegalArgumentException("El ID del paciente no puede ser nulo o negativo.");
        }

        LocalDate fechaCita = LocalDate.parse(fechaStr);
        LocalTime horaCita = LocalTime.parse(horaStr);

        logger.info("Intentando crear cita para fecha: {}, hora: {}, medicoId: {}, pacienteId: {}", fechaCita, horaCita, idMedico, idPaciente);

        Paciente paciente = pacienteRepositorio.findById(idPaciente)
                                .orElseThrow(() -> new IllegalArgumentException("Paciente con ID " + idPaciente + " no encontrado."));
        logger.debug("Paciente obtenido para cita: {}", paciente.getId());

        Medico medico = medicoRepositorio.findById(idMedico)
                                .orElseThrow(() -> new IllegalArgumentException("Médico con ID " + idMedico + " no encontrado."));
        logger.debug("Médico obtenido para cita: {}", medico.getId());

        Optional<Horario> horarioOptional = horarioRepositorio.findByFechaAndHoraAndMedico(fechaCita, horaCita, medico);
        Horario horario = horarioOptional.orElseThrow(() -> new IllegalArgumentException("Horario disponible no encontrado para la fecha y hora especificadas."));

        if (!horario.isDisponible()) {
            throw new IllegalStateException("El horario seleccionado ya no está disponible.");
        }

        Cita nuevaCita = new Cita();
        nuevaCita.setFecha(fechaCita);
        nuevaCita.setHora(horaCita);
        nuevaCita.setEstado("Pendiente");
        nuevaCita.setPaciente(paciente);
        nuevaCita.setMedico(medico);

        horario.setDisponible(false);
        horarioRepositorio.save(horario);

        Cita citaGuardada = citaRepositorio.save(nuevaCita);
        logger.info("Cita guardada exitosamente con ID: {}", citaGuardada.getId());

        return citaGuardada;
    }

    public List<Cita> obtenerTodasLasCitas() {
        return citaRepositorio.findAll();
    }

    public List<Cita> obtenerCitasPendientesPorPaciente(Long idPaciente) {
        Paciente paciente = pacienteRepositorio.findById(idPaciente)
                .orElseThrow(() -> new IllegalArgumentException("Paciente con ID " + idPaciente + " no encontrado."));

        return citaRepositorio.findByPacienteAndEstado(paciente, "Pendiente");
    }

    public List<Cita> obtenerHistorialCitasPorPaciente(Long idPaciente) {
        Paciente paciente = pacienteRepositorio.findById(idPaciente)
                .orElseThrow(() -> new IllegalArgumentException("Paciente con ID " + idPaciente + " no encontrado."));

        return citaRepositorio.findByPacienteAndEstadoNot(paciente, "Pendiente");
    }

    @Transactional
    public boolean cancelarCita(Long idCita) {
        Optional<Cita> citaOptional = citaRepositorio.findById(idCita);

        if (citaOptional.isPresent()) {
            Cita cita = citaOptional.get();
            cita.setEstado("Cancelada");
            citaRepositorio.save(cita);

            Optional<Horario> horarioOptional = horarioRepositorio.findByFechaAndHoraAndMedico(cita.getFecha(), cita.getHora(), cita.getMedico());
            if (horarioOptional.isPresent()) {
                Horario horario = horarioOptional.get();
                horario.setDisponible(true);
                horarioRepositorio.save(horario);
                logger.info("Horario {}-{}-{} del médico {} marcado como disponible tras cancelación de cita {}",
                            horario.getFecha(), horario.getHora(), horario.getMedico().getId(), idCita);
            } else {
                logger.warn("No se encontró el horario asociado a la cita ID {} para re-marcarlo como disponible.", idCita);
            }
            logger.info("Cita ID {} cancelada exitosamente.", idCita);
            return true;
        } else {
            logger.warn("Intento de cancelar cita con ID {} fallido: Cita no encontrada.", idCita);
            return false;
        }
    }

    public List<Especialidad> obtenerTodasLasEspecialidades() {
        logger.info("Cargando todas las especialidades desde CitaServicio.");
        return especialidadRepositorio.findAll();
    }

    public List<Medico> obtenerMedicosPorEspecialidad(Long idEspecialidad) {
        if (idEspecialidad == null || idEspecialidad <= 0) {
            throw new IllegalArgumentException("El ID de la especialidad no puede ser nulo o negativo.");
        }
        logger.info("Buscando médicos para la especialidad ID: {} desde CitaServicio.", idEspecialidad);

        Especialidad especialidad = especialidadRepositorio.findById(idEspecialidad)
                                            .orElseThrow(() -> new IllegalArgumentException("Especialidad con ID " + idEspecialidad + " no encontrada."));

        return medicoRepositorio.findByEspecialidad(especialidad);
    }

    public List<Horario> obtenerHorariosDisponiblesPorMedicoYFecha(Long idMedico, LocalDate fecha) {
        Medico medico = medicoRepositorio.findById(idMedico)
                                .orElseThrow(() -> new IllegalArgumentException("Médico con ID " + idMedico + " no encontrado."));

        return horarioRepositorio.findByMedicoAndFechaAndDisponibleTrue(medico, fecha);
    }
}