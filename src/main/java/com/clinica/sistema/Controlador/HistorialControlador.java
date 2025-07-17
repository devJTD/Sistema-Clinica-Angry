package com.clinica.sistema.Controlador;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.clinica.sistema.Modelo.Cita;
import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Servicio.AuthServicio;
import com.clinica.sistema.Servicio.CitaServicio;

@Controller
public class HistorialControlador {

    private final Logger logger = LoggerFactory.getLogger(HistorialControlador.class);

    private final CitaServicio citaServicio;
    private final AuthServicio authServicio;

    public HistorialControlador(CitaServicio citaServicio, AuthServicio authServicio) {
        this.citaServicio = citaServicio;
        this.authServicio = authServicio;
    }

    // Obtiene el objeto Paciente del usuario actualmente logueado.
    private Paciente getPacienteLogueado() {
        // Obtiene la información de autenticación del contexto de seguridad de Spring.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Verifica si el usuario está autenticado y si el principal es un objeto UserDetails (no una cadena anónima).
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() instanceof String) {
            logger.debug("No hay usuario autenticado o el principal no es un UserDetails.");
            return null;
        }

        // Extrae los detalles del usuario y su correo electrónico.
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String correoUsuario = userDetails.getUsername();
        logger.debug("Buscando paciente logueado con correo: {}", correoUsuario);

        // Busca el paciente en la base de datos usando el correo.
        Optional<Paciente> pacienteOpt = authServicio.buscarPorCorreo(correoUsuario);
        if (pacienteOpt.isPresent()) {
            logger.debug("Paciente logueado encontrado con ID: {} y correo: {}", pacienteOpt.get().getId(), correoUsuario);
        } else {
            logger.warn("No se encontro paciente en la base de datos para el correo: {}", correoUsuario);
        }
        return pacienteOpt.orElse(null);
    }

    // Muestra la página del historial de citas del paciente, incluyendo citas pendientes y pasadas.
    @GetMapping("/historial")
    public String mostrarPaginaHistorialCitas(Model model) {
        logger.info("Accediendo a la pagina de historial de citas.");
        // Obtiene el paciente logueado.
        Paciente usuario = getPacienteLogueado();
        // Si no hay un usuario logueado o su ID es nulo, redirige a la página de login.
        if (usuario == null || usuario.getId() == null) {
            logger.warn("Usuario no logueado intento acceder a la pagina de historial de citas. Redirigiendo a login.");
            return "redirect:/login?error=nologin";
        }

        logger.info("Usuario con DNI: {} ({}) ha accedido a la pagina de historial de citas.", usuario.getDni(), usuario.getCorreo());

        try {
            // Obtiene TODAS las citas del paciente, incluyendo las pendientes y ya pasadas.
            List<Cita> todasLasCitas = citaServicio.obtenerTodasLasCitasPorPaciente(usuario.getId());
            logger.debug("Se recuperaron {} citas totales para el usuario con DNI: {}.", todasLasCitas.size(), usuario.getDni());

            LocalDate fechaActual = LocalDate.now();
            LocalTime horaActual = LocalTime.now();
            
            List<Cita> citasActualizadas = new ArrayList<>();
            List<Cita> citasPendientes = new ArrayList<>();
            List<Cita> historialCitas = new ArrayList<>();

            // Itera sobre todas las citas para verificar y actualizar su estado si es necesario.
            for (Cita cita : todasLasCitas) {
                // Solo actualiza citas que están "Pendiente" y cuya fecha/hora ya pasó.
                if ("Pendiente".equals(cita.getEstado()) && 
                    (cita.getFecha().isBefore(fechaActual) || 
                    (cita.getFecha().isEqual(fechaActual) && cita.getHora().isBefore(horaActual)))) {
                    
                    cita.setEstado("Completada"); // Cambia el estado a "Completada"
                    citasActualizadas.add(cita); // Añade la cita a la lista de citas a actualizar
                    logger.info("Cita ID {} (Fecha: {}, Hora: {}) del paciente ID {} marcada como 'Completada' porque ha pasado su hora.", cita.getId(), cita.getFecha(), cita.getHora(), usuario.getId());
                }

                // Clasifica la cita en la lista correspondiente (pendientes o historial).
                if ("Pendiente".equals(cita.getEstado())) {
                    citasPendientes.add(cita);
                } else {
                    historialCitas.add(cita);
                }
            }

            // Guarda todas las citas que fueron actualizadas en la base de datos en una sola transacción.
            if (!citasActualizadas.isEmpty()) {
                citaServicio.guardarCitas(citasActualizadas);
                logger.info("Se actualizaron {} citas a estado 'Completada' en la base de datos para el paciente ID {}.", citasActualizadas.size(), usuario.getId());
            }

            // Ordena las citas pendientes por fecha y hora (ascendente)
            citasPendientes.sort(Comparator
                .comparing(Cita::getFecha)
                .thenComparing(Cita::getHora));
            
            // Ordena las citas de historial por fecha y hora (descendente)
            historialCitas.sort(Comparator
                .comparing(Cita::getFecha, Comparator.reverseOrder())
                .thenComparing(Cita::getHora, Comparator.reverseOrder()));

            // Añade los atributos al modelo para que la vista los muestre.
            model.addAttribute("citasPendientes", citasPendientes);
            model.addAttribute("historialCitas", historialCitas);
            model.addAttribute("nombreUsuario", usuario.getNombre() + " " + usuario.getApellido());
            logger.info("Citas pendientes: {} y historial de citas: {} cargados y actualizados para el usuario con DNI: {}.", citasPendientes.size(), historialCitas.size(), usuario.getDni());

        } catch (IllegalArgumentException e) {
            logger.error("Error al cargar las citas del usuario con DNI: {}: {}", usuario.getDni(), e.getMessage());
            model.addAttribute("error", "Error al cargar sus citas: " + e.getMessage());
            model.addAttribute("citasPendientes", List.of());
            model.addAttribute("historialCitas", List.of());
        } catch (Exception e) {
            logger.error("Error inesperado al cargar las citas del usuario con DNI: {}: {}", usuario.getDni(), e.getMessage(), e);
            model.addAttribute("error", "Ocurrio un error inesperado al cargar sus citas.");
            model.addAttribute("citasPendientes", List.of());
            model.addAttribute("historialCitas", List.of());
        }

        return "historialCita";
    }

    // Exporta el historial completo de citas del paciente a un archivo Excel.
    @GetMapping("/historial/exportar/excel")
    public ResponseEntity<byte[]> exportarHistorialCitasExcel() {
        // Obtiene el paciente logueado.
        Paciente pacienteLogueado = getPacienteLogueado();
        // Si no está logueado, retorna un error de no autorizado.
        if (pacienteLogueado == null || pacienteLogueado.getId() == null) {
            logger.warn("Intento de exportar historial de citas por usuario no logueado o sin ID de paciente. Retornando no autorizado.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autorizado: Debes iniciar sesion para exportar.".getBytes());
        }

        logger.info("Usuario con DNI: {} ({}) ha solicitado exportar su historial de citas a Excel.", pacienteLogueado.getDni(), pacienteLogueado.getCorreo());

        try {
            // Obtiene todas las citas del paciente.
            // NOTA: Con la lógica de actualización en mostrarPaginaHistorialCitas(), las citas ya deberían estar en el estado correcto.
            List<Cita> citasDelPaciente = citaServicio.obtenerTodasLasCitasPorPaciente(pacienteLogueado.getId());
            logger.debug("Se recuperaron {} citas totales para la exportacion de Excel del usuario con DNI: {}.", citasDelPaciente.size(), pacienteLogueado.getDni());

            // Filtra y prepara las listas para la exportación.
            List<Cita> citasPendientes = citasDelPaciente.stream()
                .filter(cita -> "Pendiente".equals(cita.getEstado()))
                .collect(Collectors.toList());

            List<Cita> historialCitas = citasDelPaciente.stream()
                .filter(cita -> !"Pendiente".equals(cita.getEstado()))
                .collect(Collectors.toList());

            // Combina ambas listas para la exportación total si es necesario, o exporta por separado.
            // Para este caso, combinamos para un solo archivo de historial completo.
            List<Cita> todasLasCitasParaExportar = new ArrayList<>(citasPendientes);
            todasLasCitasParaExportar.addAll(historialCitas);
            todasLasCitasParaExportar.sort(Comparator
                .comparing(Cita::getFecha)
                .thenComparing(Cita::getHora));

            // Crea un nuevo libro de Excel y una hoja.
            try (Workbook workbook = new XSSFWorkbook();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                Sheet sheet = workbook.createSheet(
                                        "Historial Citas - " + pacienteLogueado.getNombre() + " " + pacienteLogueado.getApellido());

                // Crea la fila de encabezados para el archivo Excel.
                Row headerRow = sheet.createRow(0);
                String[] headers = { "ID Cita", "Fecha", "Hora", "Estado", "Medico", "Especialidad" };
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                }

                // Llena el resto de las filas con los datos de las citas.
                int rowNum = 1;
                for (Cita cita : todasLasCitasParaExportar) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(cita.getId());
                    row.createCell(1).setCellValue(cita.getFecha().toString());
                    row.createCell(2).setCellValue(cita.getHora().toString());
                    row.createCell(3).setCellValue(cita.getEstado());

                    if (cita.getMedico() != null) {
                        row.createCell(4).setCellValue(cita.getMedico().getNombre() + " " + cita.getMedico().getApellido());
                        if (cita.getMedico().getEspecialidad() != null) {
                            row.createCell(5).setCellValue(cita.getMedico().getEspecialidad().getNombre());
                        } else {
                            row.createCell(5).setCellValue("Sin Especialidad");
                        }
                    } else {
                        row.createCell(4).setCellValue("Medico Desconocido");
                        row.createCell(5).setCellValue("N/A");
                    }
                }

                // Ajusta automáticamente el tamaño de las columnas.
                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }

                // Escribe el contenido del libro de Excel en un flujo de salida.
                workbook.write(outputStream);
                logger.info("Reporte Excel generado exitosamente para el usuario con DNI: {}. Tamano: {} bytes.", pacienteLogueado.getDni(), outputStream.toByteArray().length);

                // Configura los encabezados HTTP para la descarga del archivo.
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                httpHeaders.setContentDispositionFormData("attachment",
                        "historial_citas_" + pacienteLogueado.getId() + ".xlsx");
                httpHeaders.setContentLength(outputStream.toByteArray().length);

                // Retorna la respuesta con el archivo Excel.
                return ResponseEntity.ok().headers(httpHeaders).body(outputStream.toByteArray());

            } catch (IOException e) {
                logger.error("Error de IO al generar el reporte Excel para el usuario con DNI: {}: {}", pacienteLogueado.getDni(), e.getMessage(), e);
                return ResponseEntity.internalServerError()
                    .body(("Error al generar el reporte: " + e.getMessage()).getBytes());
            }

        } catch (IllegalArgumentException e) {
            logger.error("Error de argumento ilegal al exportar historial de citas para el usuario con DNI: {}: {}", pacienteLogueado.getDni(), e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage().getBytes());
        } catch (Exception e) {
            logger.error("Error inesperado al exportar historial de citas para el usuario con DNI: {}: {}", pacienteLogueado.getDni(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body(("Error inesperado al generar el reporte: " + e.getMessage()).getBytes());
        }
    }

    // Permite al usuario cancelar una cita específica.
    @PostMapping("/cancelar-cita")
    @ResponseBody
    public ResponseEntity<String> cancelarCita(@RequestParam Long id) {
        logger.info("Solicitud para cancelar cita con ID: {}", id);
        try {
            // Intenta cancelar la cita usando el servicio.
            boolean exito = citaServicio.cancelarCita(id);
            if (exito) {
                logger.info("Cita con ID: {} cancelada correctamente.", id);
                return ResponseEntity.ok("Cita cancelada correctamente.");
            } else {
                logger.warn("No se encontro la cita con ID: {} para cancelar.", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No se encontro la cita para cancelar.");
            }
        } catch (IllegalArgumentException e) {
            logger.error("Error al cancelar la cita con ID: {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body("Error al cancelar la cita: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error interno al cancelar la cita con ID: {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno al cancelar la cita.");
        }
    }
}