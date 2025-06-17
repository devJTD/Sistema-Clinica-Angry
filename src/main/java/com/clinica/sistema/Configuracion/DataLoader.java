package com.clinica.sistema.Configuracion;

import com.clinica.sistema.Modelo.Especialidad;
import com.clinica.sistema.Modelo.Medico;
import com.clinica.sistema.Modelo.Horario;
import com.clinica.sistema.Repositorio.EspecialidadRepositorio;
import com.clinica.sistema.Repositorio.MedicoRepositorio;
import com.clinica.sistema.Repositorio.HorarioRepositorio;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

    private final EspecialidadRepositorio especialidadRepositorio;
    private final MedicoRepositorio medicoRepositorio;
    private final HorarioRepositorio horarioRepositorio;

    public DataLoader(EspecialidadRepositorio especialidadRepositorio,
                      MedicoRepositorio medicoRepositorio,
                      HorarioRepositorio horarioRepositorio) {
        this.especialidadRepositorio = especialidadRepositorio;
        this.medicoRepositorio = medicoRepositorio;
        this.horarioRepositorio = horarioRepositorio;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        logger.info("Iniciando carga de datos de especialidades, médicos y horarios...");

        Map<Long, Especialidad> especialidadesMap = new HashMap<>();

        List<Map<String, Object>> especialidadesData = Arrays.asList(
            Map.of("id", 1L, "nombre", "Cardiología"),
            Map.of("id", 2L, "nombre", "Dermatología"),
            Map.of("id", 3L, "nombre", "Pediatría"),
            Map.of("id", 4L, "nombre", "Ginecología"),
            Map.of("id", 5L, "nombre", "Neurología"),
            Map.of("id", 6L, "nombre", "Traumatología"),
            Map.of("id", 7L, "nombre", "Oftalmología"),
            Map.of("id", 8L, "nombre", "Otorrinolaringología"),
            Map.of("id", 9L, "nombre", "Urología"),
            Map.of("id", 10L, "nombre", "Endocrinología"),
            Map.of("id", 11L, "nombre", "Gastroenterología"),
            Map.of("id", 12L, "nombre", "Oncología"),
            Map.of("id", 13L, "nombre", "Psicología"),
            Map.of("id", 14L, "nombre", "Odontología"),
            Map.of("id", 15L, "nombre", "Medicina Interna")
        );

        logger.info("Procesando {} especialidades...", especialidadesData.size());
        for (Map<String, Object> data : especialidadesData) {
            String nombreEspecialidad = (String) data.get("nombre");
            Long idReferencia = (Long) data.get("id");

            Optional<Especialidad> existingEspecialidad = especialidadRepositorio.findByNombre(nombreEspecialidad);
            Especialidad especialidad;

            if (existingEspecialidad.isPresent()) {
                especialidad = existingEspecialidad.get();
                logger.debug("Especialidad '{}' ya existe (ID: {}). No se creará nuevamente.", especialidad.getNombre(), especialidad.getId());
            } else {
                especialidad = new Especialidad();
                especialidad.setNombre(nombreEspecialidad);
                especialidad = especialidadRepositorio.save(especialidad);
                logger.info("Especialidad '{}' creada con ID: {}.", especialidad.getNombre(), especialidad.getId());
            }
            especialidadesMap.put(idReferencia, especialidad);
        }
        logger.info("Especialidades procesadas.");

        Map<Long, Medico> medicosMap = new HashMap<>();

        List<Map<String, Object>> medicosData = Arrays.asList(
            Map.of("id", 101L, "nombreCompleto", "Dr. Juan Pérez", "idEspecialidad", 1L),
            Map.of("id", 102L, "nombreCompleto", "Dra. María López", "idEspecialidad", 1L),
            Map.of("id", 103L, "nombreCompleto", "Dr. Carlos Sánchez", "idEspecialidad", 1L),
            Map.of("id", 104L, "nombreCompleto", "Dra. Ana Torres", "idEspecialidad", 2L),
            Map.of("id", 105L, "nombreCompleto", "Dr. Ricardo Gómez", "idEspecialidad", 2L),
            Map.of("id", 106L, "nombreCompleto", "Dra. Laura Castillo", "idEspecialidad", 2L),
            Map.of("id", 107L, "nombreCompleto", "Dr. Luis Morales", "idEspecialidad", 3L),
            Map.of("id", 108L, "nombreCompleto", "Dra. Cecilia Ramírez", "idEspecialidad", 3L),
            Map.of("id", 109L, "nombreCompleto", "Dr. Ernesto Vega", "idEspecialidad", 3L),
            Map.of("id", 110L, "nombreCompleto", "Dra. Patricia Herrera", "idEspecialidad", 4L),
            Map.of("id", 111L, "nombreCompleto", "Dr. Miguel Silva", "idEspecialidad", 4L),
            Map.of("id", 112L, "nombreCompleto", "Dra. Andrea Mendoza", "idEspecialidad", 4L),
            Map.of("id", 113L, "nombreCompleto", "Dr. Fernando Ruiz", "idEspecialidad", 5L),
            Map.of("id", 114L, "nombreCompleto", "Dra. Verónica Aguilar", "idEspecialidad", 5L),
            Map.of("id", 115L, "nombreCompleto", "Dr. Nicolás Rivas", "idEspecialidad", 5L),
            Map.of("id", 116L, "nombreCompleto", "Dra. Sonia Navarro", "idEspecialidad", 6L),
            Map.of("id", 117L, "nombreCompleto", "Dr. Diego Salazar", "idEspecialidad", 6L),
            Map.of("id", 118L, "nombreCompleto", "Dr. Alfredo Guzmán", "idEspecialidad", 6L),
            Map.of("id", 119L, "nombreCompleto", "Dra. Clara Ortega", "idEspecialidad", 7L),
            Map.of("id", 120L, "nombreCompleto", "Dr. Tomás Cáceres", "idEspecialidad", 7L),
            Map.of("id", 121L, "nombreCompleto", "Dra. Rebeca Lozano", "idEspecialidad", 7L),
            Map.of("id", 122L, "nombreCompleto", "Dr. Iván Herrera", "idEspecialidad", 8L),
            Map.of("id", 123L, "nombreCompleto", "Dra. Elsa Fernández", "idEspecialidad", 8L),
            Map.of("id", 124L, "nombreCompleto", "Dr. Pablo León", "idEspecialidad", 8L),
            Map.of("id", 125L, "nombreCompleto", "Dra. Marta Reyes", "idEspecialidad", 9L),
            Map.of("id", 126L, "nombreCompleto", "Dr. Alejandro Bustamante", "idEspecialidad", 9L),
            Map.of("id", 127L, "nombreCompleto", "Dr. Javier Andrade", "idEspecialidad", 9L),
            Map.of("id", 128L, "nombreCompleto", "Dr. Andrés Paredes", "idEspecialidad", 10L),
            Map.of("id", 129L, "nombreCompleto", "Dra. Paola Méndez", "idEspecialidad", 10L),
            Map.of("id", 130L, "nombreCompleto", "Dr. Germán Palacios", "idEspecialidad", 10L),
            Map.of("id", 131L, "nombreCompleto", "Dra. Lorena Ibáñez", "idEspecialidad", 11L),
            Map.of("id", 132L, "nombreCompleto", "Dr. Marcelo Castro", "idEspecialidad", 11L),
            Map.of("id", 133L, "nombreCompleto", "Dra. Karina Soto", "idEspecialidad", 11L),
            Map.of("id", 134L, "nombreCompleto", "Dr. Sergio Medina", "idEspecialidad", 12L),
            Map.of("id", 135L, "nombreCompleto", "Dra. Teresa Alarcón", "idEspecialidad", 12L),
            Map.of("id", 136L, "nombreCompleto", "Dr. Hugo Céspedes", "idEspecialidad", 12L),
            Map.of("id", 137L, "nombreCompleto", "Dra. Daniela Pinto", "idEspecialidad", 13L),
            Map.of("id", 138L, "nombreCompleto", "Dr. Esteban Vargas", "idEspecialidad", 13L),
            Map.of("id", 139L, "nombreCompleto", "Dra. Camila Ponce", "idEspecialidad", 13L),
            Map.of("id", 140L, "nombreCompleto", "Dr. Mauricio Bravo", "idEspecialidad", 14L),
            Map.of("id", 141L, "nombreCompleto", "Dra. Beatriz Molina", "idEspecialidad", 14L),
            Map.of("id", 142L, "nombreCompleto", "Dr. Hugo Ramírez", "idEspecialidad", 14L),
            Map.of("id", 143L, "nombreCompleto", "Dr. Felipe Aguilar", "idEspecialidad", 15L),
            Map.of("id", 144L, "nombreCompleto", "Dra. Gabriela Cáceres", "idEspecialidad", 15L),
            Map.of("id", 145L, "nombreCompleto", "Dr. Vicente Romero", "idEspecialidad", 15L)
        );

        logger.info("Procesando {} médicos...", medicosData.size());
        for (Map<String, Object> data : medicosData) {
            String nombreCompleto = (String) data.get("nombreCompleto");
            Long idEspecialidadRef = (Long) data.get("idEspecialidad");
            Long idReferenciaMedico = (Long) data.get("id");

            String nombre;
            String apellido;
            String cleanedNombre = nombreCompleto.replace("Dr. ", "").replace("Dra. ", "").trim();
            int lastSpaceIndex = cleanedNombre.lastIndexOf(" ");

            if (lastSpaceIndex != -1) {
                nombre = cleanedNombre.substring(0, lastSpaceIndex);
                apellido = cleanedNombre.substring(lastSpaceIndex + 1);
            } else {
                nombre = cleanedNombre;
                apellido = "";
                logger.warn("No se pudo dividir el nombre completo: '{}'. Usando '{}' como nombre y apellido vacío.", nombreCompleto, nombre);
            }

            Optional<Medico> existingMedico = medicoRepositorio.findByNombreAndApellido(nombre, apellido);
            Medico medico;

            if (existingMedico.isPresent()) {
                medico = existingMedico.get();
                logger.debug("Médico '{} {}' ya existe (ID: {}). No se creará nuevamente.", medico.getNombre(), medico.getApellido(), medico.getId());
            } else {
                medico = new Medico();
                medico.setNombre(nombre);
                medico.setApellido(apellido);
                
                Especialidad especialidad = especialidadesMap.get(idEspecialidadRef);
                if (especialidad == null) {
                    logger.error("No se encontró especialidad con ID de referencia {} para el médico {}. Este médico se creará sin especialidad.", idEspecialidadRef, nombreCompleto);
                }
                medico.setEspecialidad(especialidad);
                medico = medicoRepositorio.save(medico);
                logger.info("Médico '{} {}' creado con ID: {}.", medico.getNombre(), medico.getApellido(), medico.getId());
            }
            medicosMap.put(idReferenciaMedico, medico);
        }
        logger.info("Médicos procesados.");

        LocalDate startDate = LocalDate.of(2025, 6, 19);
        LocalDate endDate = LocalDate.of(2025, 6, 21);
        List<LocalTime> horasDisponibles = Arrays.asList(
            LocalTime.of(9, 0),
            LocalTime.of(11, 0),
            LocalTime.of(15, 0)
        );

        logger.info("Procesando horarios del {} al {} para cada médico...", startDate, endDate);
        int horariosCreadosCount = 0;
        for (Medico medico : medicosMap.values()) {
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                for (LocalTime time : horasDisponibles) {
                    Optional<Horario> existingHorario = horarioRepositorio.findByMedicoAndFechaAndHora(medico, date, time);
                    if (existingHorario.isPresent()) {
                        logger.debug("Horario para médico ID {} en {} a las {} ya existe. No se creará nuevamente.", medico.getId(), date, time);
                        continue;
                    }

                    Horario horario = new Horario();
                    horario.setMedico(medico);
                    horario.setFecha(date);
                    horario.setHora(time);
                    horario.setDisponible(true);
                    horarioRepositorio.save(horario);
                    horariosCreadosCount++;
                }
            }
        }
        logger.info("Total de horarios procesados: {}", horariosCreadosCount);

        logger.info("Carga de datos iniciales completada.");
    }
}