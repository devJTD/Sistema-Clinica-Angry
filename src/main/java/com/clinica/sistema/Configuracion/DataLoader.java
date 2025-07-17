package com.clinica.sistema.Configuracion;

import com.clinica.sistema.Modelo.Direccion;
import com.clinica.sistema.Modelo.Especialidad;
import com.clinica.sistema.Modelo.Horario;
import com.clinica.sistema.Modelo.Medico;
import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Repositorio.EspecialidadRepositorio;
import com.clinica.sistema.Repositorio.HorarioRepositorio;
import com.clinica.sistema.Repositorio.MedicoRepositorio;
import com.clinica.sistema.Repositorio.PacienteRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
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
    private final PacienteRepositorio pacienteRepositorio;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(EspecialidadRepositorio especialidadRepositorio,
                      MedicoRepositorio medicoRepositorio,
                      HorarioRepositorio horarioRepositorio,
                      PacienteRepositorio pacienteRepositorio,
                      PasswordEncoder passwordEncoder) {
        this.especialidadRepositorio = especialidadRepositorio;
        this.medicoRepositorio = medicoRepositorio;
        this.horarioRepositorio = horarioRepositorio;
        this.pacienteRepositorio = pacienteRepositorio;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        logger.info("Iniciando carga de datos de especialidades, medicos, pacientes y horarios...");

        Map<Long, Especialidad> especialidadesMap = new HashMap<>();

        List<Map<String, Object>> especialidadesData = Arrays.asList(
                Map.of("id", 1L, "nombre", "Cardiologia"),
                Map.of("id", 2L, "nombre", "Dermatologia"),
                Map.of("id", 3L, "nombre", "Pediatria"),
                Map.of("id", 4L, "nombre", "Ginecologia"),
                Map.of("id", 5L, "nombre", "Neurologia"),
                Map.of("id", 6L, "nombre", "Traumatologia"),
                Map.of("id", 7L, "nombre", "Oftalmologia"),
                Map.of("id", 8L, "nombre", "Otorrinolaringologia"),
                Map.of("id", 9L, "nombre", "Urologia"),
                Map.of("id", 10L, "nombre", "Endocrinologia"),
                Map.of("id", 11L, "nombre", "Gastroenterologia"),
                Map.of("id", 12L, "nombre", "Oncologia"),
                Map.of("id", 13L, "nombre", "Psicologia"),
                Map.of("id", 14L, "nombre", "Odontologia"),
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
                Map.of("id", 101L, "nombreCompleto", "Dr. Juan Perez", "idEspecialidad", 1L),
                Map.of("id", 102L, "nombreCompleto", "Dra. Maria Lopez", "idEspecialidad", 1L),
                Map.of("id", 103L, "nombreCompleto", "Dr. Carlos Sanchez", "idEspecialidad", 1L),
                Map.of("id", 104L, "nombreCompleto", "Dra. Ana Torres", "idEspecialidad", 2L),
                Map.of("id", 105L, "nombreCompleto", "Dr. Ricardo Gomez", "idEspecialidad", 2L),
                Map.of("id", 106L, "nombreCompleto", "Dra. Laura Castillo", "idEspecialidad", 2L),
                Map.of("id", 107L, "nombreCompleto", "Dr. Luis Morales", "idEspecialidad", 3L),
                Map.of("id", 108L, "nombreCompleto", "Dra. Cecilia Ramirez", "idEspecialidad", 3L),
                Map.of("id", 109L, "nombreCompleto", "Dr. Ernesto Vega", "idEspecialidad", 3L),
                Map.of("id", 110L, "nombreCompleto", "Dra. Patricia Herrera", "idEspecialidad", 4L),
                Map.of("id", 111L, "nombreCompleto", "Dr. Miguel Silva", "idEspecialidad", 4L),
                Map.of("id", 112L, "nombreCompleto", "Dra. Andrea Mendoza", "idEspecialidad", 4L),
                Map.of("id", 113L, "nombreCompleto", "Dr. Fernando Ruiz", "idEspecialidad", 5L),
                Map.of("id", 114L, "nombreCompleto", "Dra. Veronica Aguilar", "idEspecialidad", 5L),
                Map.of("id", 115L, "nombreCompleto", "Dr. Nicolas Rivas", "idEspecialidad", 5L),
                Map.of("id", 116L, "nombreCompleto", "Dra. Sonia Navarro", "idEspecialidad", 6L),
                Map.of("id", 117L, "nombreCompleto", "Dr. Diego Salazar", "idEspecialidad", 6L),
                Map.of("id", 118L, "nombreCompleto", "Dr. Alfredo Guzman", "idEspecialidad", 6L),
                Map.of("id", 119L, "nombreCompleto", "Dra. Clara Ortega", "idEspecialidad", 7L),
                Map.of("id", 120L, "nombreCompleto", "Dr. Tomas Caceres", "idEspecialidad", 7L),
                Map.of("id", 121L, "nombreCompleto", "Dra. Rebeca Lozano", "idEspecialidad", 7L),
                Map.of("id", 122L, "nombreCompleto", "Dr. Ivan Herrera", "idEspecialidad", 8L),
                Map.of("id", 123L, "nombreCompleto", "Dra. Elsa Fernandez", "idEspecialidad", 8L),
                Map.of("id", 124L, "nombreCompleto", "Dr. Pablo Leon", "idEspecialidad", 8L),
                Map.of("id", 125L, "nombreCompleto", "Dra. Marta Reyes", "idEspecialidad", 9L),
                Map.of("id", 126L, "nombreCompleto", "Dr. Alejandro Bustamante", "idEspecialidad", 9L),
                Map.of("id", 127L, "nombreCompleto", "Dr. Javier Andrade", "idEspecialidad", 9L),
                Map.of("id", 128L, "nombreCompleto", "Dr. Andres Paredes", "idEspecialidad", 10L),
                Map.of("id", 129L, "nombreCompleto", "Dra. Paola Mendez", "idEspecialidad", 10L),
                Map.of("id", 130L, "nombreCompleto", "Dr. German Palacios", "idEspecialidad", 10L),
                Map.of("id", 131L, "nombreCompleto", "Dra. Lorena Ibañez", "idEspecialidad", 11L),
                Map.of("id", 132L, "nombreCompleto", "Dr. Marcelo Castro", "idEspecialidad", 11L),
                Map.of("id", 133L, "nombreCompleto", "Dra. Karina Soto", "idEspecialidad", 11L),
                Map.of("id", 134L, "nombreCompleto", "Dr. Sergio Medina", "idEspecialidad", 12L),
                Map.of("id", 135L, "nombreCompleto", "Dra. Teresa Alarcon", "idEspecialidad", 12L),
                Map.of("id", 136L, "nombreCompleto", "Dr. Hugo Cespedes", "idEspecialidad", 12L),
                Map.of("id", 137L, "nombreCompleto", "Dra. Daniela Pinto", "idEspecialidad", 13L),
                Map.of("id", 138L, "nombreCompleto", "Dr. Esteban Vargas", "idEspecialidad", 13L),
                Map.of("id", 139L, "nombreCompleto", "Dra. Camila Ponce", "idEspecialidad", 13L),
                Map.of("id", 140L, "nombreCompleto", "Dr. Mauricio Bravo", "idEspecialidad", 14L),
                Map.of("id", 141L, "nombreCompleto", "Dra. Beatriz Molina", "idEspecialidad", 14L),
                Map.of("id", 142L, "nombreCompleto", "Dr. Hugo Ramirez", "idEspecialidad", 14L),
                Map.of("id", 143L, "nombreCompleto", "Dr. Felipe Aguilar", "idEspecialidad", 15L),
                Map.of("id", 144L, "nombreCompleto", "Dra. Gabriela Caceres", "idEspecialidad", 15L),
                Map.of("id", 145L, "nombreCompleto", "Dr. Vicente Romero", "idEspecialidad", 15L)
        );

        logger.info("Procesando {} medicos...", medicosData.size());
        int medicosNuevosCount = 0;
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
                logger.warn("No se pudo dividir el nombre completo: '{}'. Usando '{}' como nombre y apellido vacio.", nombreCompleto, nombre);
            }

            Optional<Medico> existingMedico = medicoRepositorio.findByNombreAndApellido(nombre, apellido);
            Medico medico;

            if (existingMedico.isPresent()) {
                medico = existingMedico.get();
            } else {
                medico = new Medico();
                medico.setNombre(nombre);
                medico.setApellido(apellido);

                Especialidad especialidad = especialidadesMap.get(idEspecialidadRef);
                if (especialidad == null) {
                    logger.error("No se encontro especialidad con ID de referencia {} para el medico {}. Este medico se creara sin especialidad.", idEspecialidadRef, nombreCompleto);
                }
                medico.setEspecialidad(especialidad);
                medico = medicoRepositorio.save(medico);
                medicosNuevosCount++;
            }
            medicosMap.put(idReferenciaMedico, medico);
        }
        logger.info("Medicos procesados. Nuevos medicos creados: {}", medicosNuevosCount);

        logger.info("Iniciando carga de datos de pacientes...");
        List<Map<String, String>> pacientesData = Arrays.asList(
                Map.of("nombre", "Jeanpierre", "apellido", "Chipa", "dni", "71632364", "telefono", "921340064", "correo", "chipa.robladillo@gmail.com", "contraseña", "jeanpierre", "direccion", "Av. San Juan 123, Urb. Los Olivos"),
                Map.of("nombre", "Ariana", "apellido", "Davila", "dni", "71631245", "telefono", "987074242", "correo", "ariana.davila@gmail.com", "contraseña", "ariana", "direccion", "Calle Las Acacias 456, San Isidro"),
                Map.of("nombre", "Jeremy", "apellido", "Rojas", "dni", "71645689", "telefono", "987456321", "correo", "jeremy.rojas@gmail.com", "contraseña", "jeremy", "direccion", "Jr. La Molina 789, Santiago de Surco")
        );

        int pacientesNuevosCount = 0;
        for (Map<String, String> data : pacientesData) {
            String dni = data.get("dni");
            String correo = data.get("correo");
            String direccionCompleta = data.get("direccion");

            Optional<Paciente> existingPacienteDni = pacienteRepositorio.findByDni(dni);
            Optional<Paciente> existingPacienteCorreo = pacienteRepositorio.findByCorreo(correo);

            if (existingPacienteDni.isPresent() || existingPacienteCorreo.isPresent()) {
                logger.info("Paciente con DNI '{}' o correo '{}' ya existe. Omitiendo creacion.", dni, correo);
                continue;
            }

            Paciente paciente = new Paciente();
            paciente.setNombre(data.get("nombre"));
            paciente.setApellido(data.get("apellido"));
            paciente.setDni(dni);
            paciente.setTelefono(data.get("telefono"));
            paciente.setCorreo(correo);
            paciente.setContraseña(passwordEncoder.encode(data.get("contraseña"))); 
            
            // Inicializa la lista de direcciones si es nula
            if (paciente.getDirecciones() == null) {
                paciente.setDirecciones(new ArrayList<>());
                logger.debug("Inicializando lista de direcciones para el paciente con DNI: {}.", paciente.getDni());
            }

            // Crea la instancia de Direccion
            Direccion direccion = new Direccion();
            direccion.setDireccionCompleta(direccionCompleta);
            
            // Asocia la direccion al paciente (establece la relacion bidireccional)
            direccion.setPaciente(paciente);
            
            // Anade la direccion a la coleccion de direcciones del paciente
            paciente.getDirecciones().add(direccion);
            logger.debug("Asignando direccion al paciente con DNI: {}. Direccion: {}", paciente.getDni(), direccion.getDireccionCompleta());

            pacienteRepositorio.save(paciente);
            
            pacientesNuevosCount++;
            logger.info("Paciente '{} {}' creado con DNI: {}.", paciente.getNombre(), paciente.getApellido(), paciente.getDni());
        }
        logger.info("Pacientes procesados. Nuevos pacientes creados: {}", pacientesNuevosCount);

        LocalDate startDate = LocalDate.of(2025, 7, 15);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        List<LocalTime> horasDisponibles = Arrays.asList(
                LocalTime.of(9, 0),
                LocalTime.of(11, 0),
                LocalTime.of(15, 0)
        );

        logger.info("Procesando horarios del {} al {} para cada medico...", startDate, endDate);

        long existingHorariosCount = horarioRepositorio.count();
        final long HORARIOS_MIN_PARA_OMITIR = 100; // Define el umbral de horarios existentes

        if (existingHorariosCount >= HORARIOS_MIN_PARA_OMITIR) {
            logger.info("Se encontraron {} horarios existentes (mayor o igual a {}). Asumiendo que los horarios ya estan cargados y omitiendo la creacion detallada.",
                    existingHorariosCount, HORARIOS_MIN_PARA_OMITIR);
        } else {
            logger.info("Se encontraron {} horarios existentes (menos de {}). Procediendo con la verificacion y posible creacion de horarios.",
                    existingHorariosCount, HORARIOS_MIN_PARA_OMITIR);

            int horariosCreadosCount = 0;
            for (Medico medico : medicosMap.values()) {
                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                    for (LocalTime time : horasDisponibles) {
                        Optional<Horario> existingHorario = horarioRepositorio.findByMedicoAndFechaAndHora(medico, date, time);
                        if (existingHorario.isPresent()) {
                            logger.debug("Horario para el medico '{} {}' en la fecha '{}' a las '{}' ya existe. Omitiendo creacion.",
                                medico.getNombre(), medico.getApellido(), date, time);
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
            logger.info("Total de horarios nuevos creados en esta ejecucion: {}", horariosCreadosCount);
        }

        logger.info("Carga de datos iniciales completada.");
    }
}