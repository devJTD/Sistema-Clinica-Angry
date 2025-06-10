package com.clinica.sistema.Servicio;

import com.clinica.sistema.Modelo.Paciente;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;


@Service
public class AuthServicio {

    private final String rutaPacientes = "data/pacientes.json";
    private final File pacientesFile;

    private final ObjectMapper mapper = new ObjectMapper();
    private final Logger logger = LoggerFactory.getLogger(AuthServicio.class);

    private final LoadingCache<String, List<Paciente>> pacientesCache;

    public AuthServicio() throws IOException {
        File dataDirectory = new File("data");
        if (!dataDirectory.exists()) {
            boolean created = dataDirectory.mkdirs();
            if (created) {
                logger.info("Directorio 'data' creado en: {}", dataDirectory.getAbsolutePath());
            } else {
                logger.warn("No se pudo crear el directorio 'data' en: {}", dataDirectory.getAbsolutePath());
            }
        }
        
        this.pacientesFile = new File(rutaPacientes);

        if (!pacientesFile.exists()) {
            FileUtils.writeStringToFile(pacientesFile, "[]", StandardCharsets.UTF_8);
            logger.info("Archivo pacientes.json creado en: {}", pacientesFile.getAbsolutePath());
        } else {
            logger.info("Archivo pacientes.json encontrado en: {}", pacientesFile.getAbsolutePath());
        }

        this.pacientesCache = CacheBuilder.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build(new CacheLoader<String, List<Paciente>>() {
                    @Override
                    public List<Paciente> load(String key) throws Exception {
                        return leerPacientesDesdeArchivo();
                    }
                });

        try {
            pacientesCache.get("allPacientes");
        } catch (Exception e) {
            logger.error("Error al cargar pacientes en caché al inicio: {}", e.getMessage(), e);
        }
    }

    private List<Paciente> leerPacientesDesdeArchivo() throws IOException {
        try {
            if (!pacientesFile.exists() || pacientesFile.length() == 0) {
                logger.info("El archivo pacientes.json no existe o está vacío. Retornando lista vacía.");
                return new ArrayList<>();
            }
            String jsonContent = FileUtils.readFileToString(pacientesFile, StandardCharsets.UTF_8);
            return mapper.readValue(jsonContent, new TypeReference<List<Paciente>>() {});
        } catch (IOException e) {
            logger.error("Error al leer el archivo pacientes.json desde disco: {}", e.getMessage());
            FileUtils.writeStringToFile(pacientesFile, "[]", StandardCharsets.UTF_8); 
            throw e; 
        }
    }

    public List<Paciente> listarPacientes() {
        try {
            return pacientesCache.get("allPacientes");
        } catch (Exception e) {
            logger.error("Error al obtener pacientes desde caché: {}", e.getMessage());
            try {
                return leerPacientesDesdeArchivo();
            } catch (IOException ioException) {
                logger.error("Falló el fallback de lectura directa de pacientes: {}", ioException.getMessage());
                return new ArrayList<>();
            }
        }
    }

    public boolean existePacientePorEmailODni(String email, String dni) {
        Preconditions.checkArgument(StringUtils.isNotBlank(email), "El email no puede estar vacío.");
        Preconditions.checkArgument(StringUtils.isNotBlank(dni), "El DNI no puede estar vacío.");

        boolean existe = listarPacientes().stream()
                .anyMatch(p -> p.getCorreo().equalsIgnoreCase(email) || p.getDni().equals(dni));

        if (existe) {
            logger.warn("Se encontró un paciente existente con el correo {} o DNI {}.", email, dni);
        } else {
            logger.info("No existe un paciente con el correo {} ni DNI {}. Se puede registrar.", email, dni);
        }
        return existe;
    }

    public void guardarPaciente(Paciente paciente) {
        Preconditions.checkNotNull(paciente, "El paciente a guardar no puede ser nulo.");
        Preconditions.checkArgument(StringUtils.isNotBlank(paciente.getCorreo()), "El correo del paciente no puede estar vacío.");
        Preconditions.checkArgument(StringUtils.isNotBlank(paciente.getDni()), "El DNI del paciente no puede estar vacío.");
        Preconditions.checkArgument(StringUtils.isNotBlank(paciente.getContraseña()), "La contraseña del paciente no puede estar vacía.");

        List<Paciente> pacientes = new ArrayList<>(listarPacientes());

        if (paciente.getId() == null || paciente.getId() == 0L) {
            paciente.setId(generarSiguienteId(pacientes));
        }

        pacientes.removeIf(p -> p.getId().equals(paciente.getId()));
        
        pacientes.add(paciente);

        try {
            String jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(pacientes);
            FileUtils.writeStringToFile(pacientesFile, jsonString, StandardCharsets.UTF_8);
            logger.info("Paciente guardado exitosamente con ID {} y correo {}", paciente.getId(), paciente.getCorreo());

            pacientesCache.invalidateAll();
            pacientesCache.get("allPacientes");

        } catch (IOException e) {
            logger.error("Error al guardar el paciente en el archivo JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Error al guardar el paciente en el sistema de archivos.", e);
        } catch (Exception e) {
            logger.error("Error al actualizar el caché de pacientes después de guardar: {}", e.getMessage(), e);
            throw new RuntimeException("Error interno al gestionar pacientes.", e);
        }
    }

    public Paciente buscarPorCorreoYPassword(String correo, String password) {
        Preconditions.checkArgument(StringUtils.isNotBlank(correo), "El correo no puede estar vacío para la búsqueda.");
        Preconditions.checkArgument(StringUtils.isNotBlank(password), "La contraseña no puede estar vacía para la búsqueda.");

        for (Paciente paciente : listarPacientes()) {
            if (paciente.getCorreo().equalsIgnoreCase(correo) && paciente.getContraseña().equals(password)) {
                logger.info("Paciente encontrado para login: {}", correo);
                return paciente;
            }
        }
        logger.warn("Paciente no encontrado para login con correo: {}", correo);
        return null;
    }

    private Long generarSiguienteId(List<Paciente> pacientes) {
        return pacientes.stream()
                .mapToLong(Paciente::getId)
                .max()
                .orElse(0L) + 1;
    }
}