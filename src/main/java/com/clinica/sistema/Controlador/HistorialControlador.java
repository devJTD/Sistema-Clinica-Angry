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
import org.slf4j.MDC;
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

    private static final String MDC_USER_FULL_NAME = "userFullName";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_USER_DNI = "userDni";

    public HistorialControlador(CitaServicio citaServicio, AuthServicio authServicio) {
        this.citaServicio = citaServicio;
        this.authServicio = authServicio;
    }

    // Metodo auxiliar para establecer informacion del paciente en el MDC
    private void setPacienteMDCContext(Paciente paciente) {
        if (paciente != null) {
            MDC.put(MDC_USER_FULL_NAME, paciente.getNombre() + " " + paciente.getApellido());
            MDC.put(MDC_USER_ID, String.valueOf(paciente.getId()));
            MDC.put(MDC_USER_DNI, paciente.getDni());
        }
    }

    // Metodo auxiliar para limpiar informacion del paciente del MDC
    private void clearPacienteMDCContext() {
        MDC.remove(MDC_USER_FULL_NAME);
        MDC.remove(MDC_USER_ID);
        MDC.remove(MDC_USER_DNI);
    }

    // Este metodo solo obtiene el paciente, no gestiona el MDC.
    private Paciente getPacienteLogueado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() instanceof String) {
            logger.debug("No hay usuario autenticado o el principal no es un UserDetails.");
            return null;
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String correoUsuario = userDetails.getUsername();
        logger.debug("Buscando paciente logueado con correo: {}", correoUsuario);

        Optional<Paciente> pacienteOpt = authServicio.buscarPorCorreo(correoUsuario);
        if (pacienteOpt.isPresent()) {
            Paciente paciente = pacienteOpt.get();
            // Ya no ponemos MDC aqui, lo hara el metodo del controlador que llama a esto
            logger.debug("Paciente logueado encontrado con ID: {} y correo: {}", paciente.getId(), correoUsuario);
        } else {
            logger.warn("No se encontro paciente en la base de datos para el correo: {}", correoUsuario);
        }
        return pacienteOpt.orElse(null);
    }

    @GetMapping("/historial")
    public String mostrarPaginaHistorialCitas(Model model) {
        Paciente usuario = getPacienteLogueado();

        if (usuario == null || usuario.getId() == null) {
            logger.warn("Usuario no logueado o sin ID de paciente intento acceder a la pagina de historial de citas. Redirigiendo a login.");
            return "redirect:/login?error=Sesion expirada o no iniciada. Por favor, vuelve a iniciar sesion.";
        }

        // Establecer MDC al inicio del metodo del controlador
        setPacienteMDCContext(usuario);
        logger.info("El usuario {} (ID: {}, DNI: {}) ha accedido a la pagina de historial de citas.", 
                     MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI));

        try {
            List<Cita> todasLasCitas = citaServicio.obtenerTodasLasCitasPorPaciente(usuario.getId());
            logger.debug("Se recuperaron {} citas totales para el usuario con DNI: {}.", todasLasCitas.size(), usuario.getDni());

            LocalDate fechaActual = LocalDate.now();
            LocalTime horaActual = LocalTime.now();
            
            List<Cita> citasActualizadas = new ArrayList<>();
            List<Cita> citasPendientes = new ArrayList<>();
            List<Cita> historialCitas = new ArrayList<>();

            for (Cita cita : todasLasCitas) {
                if ("Pendiente".equals(cita.getEstado()) && 
                    (cita.getFecha().isBefore(fechaActual) || 
                    (cita.getFecha().isEqual(fechaActual) && cita.getHora().isBefore(horaActual)))) {
                    
                    cita.setEstado("Completada"); 
                    citasActualizadas.add(cita); 
                    logger.info("Cita ID {} (Fecha: {}, Hora: {}) del paciente {} (ID: {}, DNI: {}) marcada como 'Completada' porque ha pasado su hora.", 
                                 cita.getId(), cita.getFecha(), cita.getHora(), MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI));
                }

                if ("Pendiente".equals(cita.getEstado())) {
                    citasPendientes.add(cita);
                } else {
                    historialCitas.add(cita);
                }
            }

            if (!citasActualizadas.isEmpty()) {
                citaServicio.guardarCitas(citasActualizadas);
                logger.info("Se actualizaron {} citas a estado 'Completada' en la base de datos para el paciente {} (ID: {}, DNI: {}).", 
                             citasActualizadas.size(), MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI));
            }

            citasPendientes.sort(Comparator
                .comparing(Cita::getFecha)
                .thenComparing(Cita::getHora));
            
            historialCitas.sort(Comparator
                .comparing(Cita::getFecha, Comparator.reverseOrder())
                .thenComparing(Cita::getHora, Comparator.reverseOrder()));

            model.addAttribute("citasPendientes", citasPendientes);
            model.addAttribute("historialCitas", historialCitas);
            model.addAttribute("nombreUsuario", usuario.getNombre() + " " + usuario.getApellido());
            logger.info("Citas pendientes: {} y historial de citas: {} cargados y actualizados para el usuario {} (ID: {}, DNI: {}).", 
                         citasPendientes.size(), historialCitas.size(), MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI));

        } catch (IllegalArgumentException e) {
            logger.error("Error al cargar las citas del usuario {} (ID: {}, DNI: {}): {}", 
                         MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI), e.getMessage());
            model.addAttribute("error", "Error al cargar sus citas: " + e.getMessage());
            model.addAttribute("citasPendientes", List.of());
            model.addAttribute("historialCitas", List.of());
        } catch (Exception e) {
            logger.error("Error inesperado al cargar las citas del usuario {} (ID: {}, DNI: {}): {}", 
                         MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI), e.getMessage(), e);
            model.addAttribute("error", "Ocurrio un error inesperado al cargar sus citas.");
            model.addAttribute("citasPendientes", List.of());
            model.addAttribute("historialCitas", List.of());
        } finally {
            // Limpiar MDC al finalizar el metodo del controlador
            clearPacienteMDCContext();
        }

        return "historialCita";
    }

    @GetMapping("/historial/exportar/excel")
    public ResponseEntity<byte[]> exportarHistorialCitasExcel() {
        Paciente pacienteLogueado = getPacienteLogueado();

        if (pacienteLogueado == null || pacienteLogueado.getId() == null) {
            logger.warn("Intento de exportar historial de citas por usuario no logueado o sin ID de paciente. Retornando no autorizado.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autorizado: Debes iniciar sesion para exportar.".getBytes());
        }

        // Establecer MDC al inicio del metodo del controlador
        setPacienteMDCContext(pacienteLogueado);
        logger.info("El usuario {} (ID: {}, DNI: {}) ha solicitado exportar su historial de citas a Excel.", 
                     MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI));

        try {
            List<Cita> citasDelPaciente = citaServicio.obtenerTodasLasCitasPorPaciente(pacienteLogueado.getId());
            logger.debug("Se recuperaron {} citas totales para la exportacion de Excel del usuario {} (ID: {}, DNI: {}).", 
                         citasDelPaciente.size(), MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI));

            List<Cita> citasPendientes = citasDelPaciente.stream()
                .filter(cita -> "Pendiente".equals(cita.getEstado()))
                .collect(Collectors.toList());

            List<Cita> historialCitas = citasDelPaciente.stream()
                .filter(cita -> !"Pendiente".equals(cita.getEstado()))
                .collect(Collectors.toList());

            List<Cita> todasLasCitasParaExportar = new ArrayList<>(citasPendientes);
            todasLasCitasParaExportar.addAll(historialCitas);
            todasLasCitasParaExportar.sort(Comparator
                .comparing(Cita::getFecha)
                .thenComparing(Cita::getHora));

            try (Workbook workbook = new XSSFWorkbook();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                Sheet sheet = workbook.createSheet(
                                     "Historial Citas - " + pacienteLogueado.getNombre() + " " + pacienteLogueado.getApellido());

                Row headerRow = sheet.createRow(0);
                String[] headers = { "ID Cita", "Fecha", "Hora", "Estado", "Medico", "Especialidad" };
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                }

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

                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }

                workbook.write(outputStream);
                logger.info("Reporte Excel generado exitosamente para el usuario {} (ID: {}, DNI: {}). Tamano: {} bytes.", 
                             MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI), outputStream.toByteArray().length);

                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                httpHeaders.setContentDispositionFormData("attachment",
                        "historial_citas_" + pacienteLogueado.getId() + ".xlsx");
                httpHeaders.setContentLength(outputStream.toByteArray().length);

                return ResponseEntity.ok().headers(httpHeaders).body(outputStream.toByteArray());

            } catch (IOException e) {
                logger.error("Error de IO al generar el reporte Excel para el usuario {} (ID: {}, DNI: {}): {}", 
                             MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI), e.getMessage(), e);
                return ResponseEntity.internalServerError()
                    .body(("Error al generar el reporte: " + e.getMessage()).getBytes());
            }

        } catch (IllegalArgumentException e) {
            logger.error("Error de argumento ilegal al exportar historial de citas para el usuario {} (ID: {}, DNI: {}): {}", 
                         MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI), e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage().getBytes());
        } catch (Exception e) {
            logger.error("Error inesperado al exportar historial de citas para el usuario {} (ID: {}, DNI: {}): {}", 
                         MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI), e.getMessage(), e);
            return ResponseEntity.internalServerError().body(("Error inesperado al generar el reporte: " + e.getMessage()).getBytes());
        } finally {
            // Limpiar MDC al finalizar el metodo del controlador
            clearPacienteMDCContext();
        }
    }

    @PostMapping("/cancelar-cita")
    @ResponseBody
    public ResponseEntity<String> cancelarCita(@RequestParam Long id) {
        Paciente pacienteLogueado = getPacienteLogueado();

        if (pacienteLogueado == null || pacienteLogueado.getId() == null) {
            logger.warn("Solicitud de cancelacion de cita con ID: {} por un usuario no logueado o sin ID de paciente.", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autorizado: Debes iniciar sesion para cancelar citas.");
        }

        // Establecer MDC al inicio del metodo del controlador
        setPacienteMDCContext(pacienteLogueado);
        
        String userDetails = "usuario " + MDC.get(MDC_USER_FULL_NAME) + " (ID: " + MDC.get(MDC_USER_ID) + ", DNI: " + MDC.get(MDC_USER_DNI) + ")";
        logger.info("Solicitud de cancelacion de cita con ID: {} por parte del {}.", id, userDetails);

        try {
            boolean exito = citaServicio.cancelarCita(id);
            if (exito) {
                logger.info("Cita con ID: {} cancelada correctamente por el {}.", id, userDetails);
                return ResponseEntity.ok("Cita cancelada correctamente.");
            } else {
                logger.warn("No se encontro la cita con ID: {} para cancelar. Solicitado por el {}.", id, userDetails);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No se encontro la cita para cancelar.");
            }
        } catch (IllegalArgumentException e) {
            logger.error("Error al cancelar la cita con ID: {} por parte del {}: {}", id, userDetails, e.getMessage());
            return ResponseEntity.badRequest().body("Error al cancelar la cita: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error interno al cancelar la cita con ID: {} por parte del {}: {}", id, userDetails, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno al cancelar la cita.");
        } finally {
            // Limpiar MDC al finalizar el metodo del controlador
            clearPacienteMDCContext();
        }
    }
}