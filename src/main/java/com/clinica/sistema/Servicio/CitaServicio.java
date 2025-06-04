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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class CitaServicio {
    private final String rutaCitas = "data/citas.json";
    private final String rutaPacientes = "data/pacientes.json";
    private final String rutaMedicos = "data/medicos.json";

    private final ObjectMapper mapper;

    public CitaServicio() {
        // Configurar ObjectMapper para Java 8 date/time
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // Leer lista de citas (devuelve ArrayList vacío si no existe o está vacío)
    public List<Cita> leerCitas() throws IOException {
        File file = new File(rutaCitas);
        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }
        return mapper.readValue(file, new TypeReference<List<Cita>>() {
        });
    }

    // Guardar lista completa en citas.json
    private void guardarCitas(List<Cita> citas) throws IOException {
        File file = new File(rutaCitas);
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, citas);
    }

    public void crearCita(String fechaStr, String horaStr, Long idMedico) throws IOException {
        // 1) Leer las citas existentes
        List<Cita> citas = leerCitas();

        // 2) Obtener primer paciente
        Paciente paciente = obtenerPrimerPaciente();

        // 3) Obtener médico por ID
        Medico medico = obtenerMedicoPorId(idMedico);

        // 4) Construir nueva Cita
        Cita cita = new Cita();
        Long nuevoId = citas.isEmpty()
                ? 1L
                : citas.stream().mapToLong(Cita::getId).max().getAsLong() + 1;
        cita.setId(nuevoId);
        cita.setFecha(LocalDate.parse(fechaStr));
        cita.setHora(LocalTime.parse(horaStr));
        cita.setEstado("Pendiente");
        cita.setPaciente(paciente);
        cita.setMedico(medico);

        // 5) Añadir y guardar
        citas.add(cita);
        guardarCitas(citas);
    }

    private Paciente obtenerPrimerPaciente() throws IOException {
        File file = new File(rutaPacientes);
        List<Paciente> pacientes = mapper.readValue(file, new TypeReference<List<Paciente>>() {
        });
        return pacientes.get(0);
    }

    private Medico obtenerMedicoPorId(Long idMedico) throws IOException {
        File file = new File(rutaMedicos);
        List<Medico> medicos = mapper.readValue(file, new TypeReference<List<Medico>>() {
        });
        return medicos.stream()
                .filter(m -> m.getId().equals(idMedico))
                .findFirst()
                .orElse(null);
    }
}
