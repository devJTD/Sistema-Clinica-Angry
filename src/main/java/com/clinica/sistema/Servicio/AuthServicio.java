package com.clinica.sistema.Servicio;

import com.clinica.sistema.Modelo.Paciente;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuthServicio {
    // Ruta de pacientes.json donde se almacenan los pacientes
    private final String ruta = "src/main/resources/data/pacientes.json";

    private final ObjectMapper mapper = new ObjectMapper();

    // Loggers para registrar eventos
    private final Logger logger = LoggerFactory.getLogger(AuthServicio.class);

    // Carga la lista de pacientes desde el archivo JSON.
    public List<Paciente> listarPacientes() {
        try {
            File file = new File(ruta);

            // Si el archivo no existe, se devuelve una lista vacía
            if (!file.exists()) {
                logger.warn("Archivo pacientes.json no encontrado, devolviendo lista vacía.");
                return new ArrayList<>();
            }

            // Leer y mapear la lista de pacientes desde el archivo JSON
            List<Paciente> pacientes = mapper.readValue(file, new TypeReference<List<Paciente>>() {
            });
            logger.info("Se han cargado {} pacientes desde el archivo JSON.", pacientes.size());
            return pacientes;

        } catch (IOException e) {
            logger.error("Error al leer el archivo pacientes.json: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // Verifica si ya existe un paciente con el correo o DNI proporcionado.
    public boolean existePacientePorEmailODni(String email, String dni) {
        boolean existe = listarPacientes().stream()
                .anyMatch(p -> p.getCorreo().equalsIgnoreCase(email) || p.getDni().equals(dni));
        if (existe) {
            logger.warn("Se encontró un paciente existente con el correo {} o DNI {}.", email, dni);
        } else {
            logger.info("No existe un paciente con el correo {} ni DNI {}. Se puede registrar.", email, dni);
        }
        return existe;
    }

    // Guarda un nuevo paciente en el archivo JSON.
    public void guardarPaciente(Paciente paciente) {
        List<Paciente> pacientes = listarPacientes();

        // Asignación rápida de ID usando la marca de tiempo
        paciente.setId(System.currentTimeMillis());
        pacientes.add(paciente);

        try {
            mapper.writeValue(new File(ruta), pacientes);
            logger.info("Paciente guardado exitosamente con ID {} y correo {}", paciente.getId(), paciente.getCorreo());
        } catch (IOException e) {
            logger.error("Error al guardar el paciente en el archivo JSON: {}", e.getMessage());
        }
    }

    public Paciente buscarPorCorreoYPassword(String correo, String password) {
        for (Paciente paciente : listarPacientes()) {
            if (paciente.getCorreo().equalsIgnoreCase(correo) && paciente.getContraseña().equals(password)) {
                return paciente;
            }
        }
        return null;
    }

}
