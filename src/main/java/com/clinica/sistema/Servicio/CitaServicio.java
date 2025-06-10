package com.clinica.sistema.Servicio;

import com.clinica.sistema.Modelo.Cita;
import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Modelo.Medico;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets; 
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional; 

import org.apache.commons.io.FileUtils; 
import org.apache.commons.lang3.StringUtils; 
import com.google.common.base.Preconditions;
import org.slf4j.Logger; 
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.stream.Collectors;

import jakarta.servlet.http.HttpSession;

@Service 
public class CitaServicio {

    private final String rutaCitas = "data/citas.json";
    private final String rutaPacientes = "data/pacientes.json";
    private final String rutaMedicos = "data/medicos.json";

    private final ObjectMapper mapper;
    private final Logger logger = LoggerFactory.getLogger(CitaServicio.class); 

    public CitaServicio() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        File dataDir = new File("data");
        if (!dataDir.exists()) {
            boolean created = dataDir.mkdirs();
            if (created) {
                logger.info("Directorio 'data' creado en: {}", dataDir.getAbsolutePath());
            } else {
                logger.warn("No se pudo crear el directorio 'data' en: {}", dataDir.getAbsolutePath());
            }
        }
        
        try {
            if (!new File(rutaCitas).exists()) FileUtils.writeStringToFile(new File(rutaCitas), "[]", StandardCharsets.UTF_8);
            if (!new File(rutaPacientes).exists()) FileUtils.writeStringToFile(new File(rutaPacientes), "[]", StandardCharsets.UTF_8);
            if (!new File(rutaMedicos).exists()) FileUtils.writeStringToFile(new File(rutaMedicos), "[]", StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Error al inicializar archivos JSON: {}", e.getMessage(), e);
        }
    }

    public List<Cita> leerCitas() throws IOException {
        File file = new File(rutaCitas);
        if (!file.exists() || file.length() == 0) {
            logger.info("El archivo de citas no existe o está vacío: {}", rutaCitas);
            return new ArrayList<>();
        }
        String jsonContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        logger.debug("Contenido de citas.json: {}", jsonContent);
        return mapper.readValue(jsonContent, new TypeReference<List<Cita>>() {});
    }

    private void guardarCitas(List<Cita> citas) throws IOException {
        File file = new File(rutaCitas);
        String jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(citas);
        FileUtils.writeStringToFile(file, jsonString, StandardCharsets.UTF_8);
        logger.info("Citas guardadas exitosamente en: {}", rutaCitas);
    }

    public void crearCita(String fechaStr, String horaStr, Long idMedico, HttpSession session, Model model) throws IOException {
        Preconditions.checkArgument(StringUtils.isNotBlank(fechaStr), "La fecha de la cita no puede estar vacía.");
        Preconditions.checkArgument(StringUtils.isNotBlank(horaStr), "La hora de la cita no puede estar vacía.");
        Preconditions.checkNotNull(idMedico, "El ID del médico no puede ser nulo.");
        Preconditions.checkArgument(idMedico > 0, "El ID del médico debe ser un valor positivo.");
        logger.info("Intentando crear cita para fecha: {}, hora: {}, medicoId: {}", fechaStr, horaStr, idMedico);

        List<Cita> citas = leerCitas();

        Paciente paciente = (Paciente) session.getAttribute("usuario");
        Preconditions.checkNotNull(paciente, "No se encontró el paciente para crear la cita. Asegúrese de que haya al menos un paciente registrado.");
        logger.debug("Paciente obtenido para cita: {}", paciente.getId());

        Medico medico = obtenerMedicoPorId(idMedico);
        Preconditions.checkNotNull(medico, "No se encontró el médico con ID: " + idMedico);
        logger.debug("Médico obtenido para cita: {}", medico.getId());

        Cita cita = new Cita();
        Long nuevoId = citas.isEmpty()
                ? 1L
                : citas.stream().mapToLong(Cita::getId).max().orElse(0L) + 1;

        cita.setId(nuevoId);
        cita.setFecha(LocalDate.parse(fechaStr));
        cita.setHora(LocalTime.parse(horaStr));
        cita.setEstado("Pendiente");
        cita.setIdPaciente(paciente.getId());
        cita.setIdMedico(medico.getId());

        logger.info("Cita construida con ID: {}, Fecha: {}, Hora: {}, Paciente: {}, Medico: {}",
                            cita.getId(), cita.getFecha(), cita.getHora(), cita.getIdPaciente(), cita.getIdMedico());

        citas.add(cita);
        guardarCitas(citas);
        logger.info("Cita guardada exitosamente.");
    }

    public Medico obtenerMedicoPorId(Long idMedico) throws IOException { // Public para posible uso externo
        Preconditions.checkNotNull(idMedico, "El ID del médico no puede ser nulo.");
        Preconditions.checkArgument(idMedico > 0, "El ID del médico debe ser positivo.");

        File file = new File(rutaMedicos);
        if (!file.exists() || file.length() == 0) {
            throw new IOException("El archivo de médicos no existe o está vacío: " + rutaMedicos);
        }
        String jsonContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        List<Medico> medicos = mapper.readValue(jsonContent, new TypeReference<List<Medico>>() {});
        
        Optional<Medico> foundMedico = medicos.stream()
                .filter(m -> m.getId().equals(idMedico))
                .findFirst();
        
        if (foundMedico.isPresent()) {
            logger.debug("Médico encontrado con ID: {}", idMedico);
            return foundMedico.get();
        } else {
            logger.warn("Médico con ID {} no encontrado.", idMedico);
            return null;
        }
    }

    public List<Medico> obtenerTodosLosMedicos() throws IOException {
        File file = new File(rutaMedicos);
        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }
        String jsonContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        return mapper.readValue(jsonContent, new TypeReference<List<Medico>>() {});
    }

    public List<Cita> obtenerCitasPorPaciente(Long idPaciente) throws IOException {
        List<Cita> todas = leerCitas();
        return todas.stream()
                .filter(c -> c.getIdPaciente() != null && c.getIdPaciente().equals(idPaciente))
                .collect(Collectors.toList());
    }
}