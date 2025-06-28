package com.clinica.sistema.Controlador;

import com.clinica.sistema.Modelo.Cita;
import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Servicio.CitaServicio;
import com.clinica.sistema.Servicio.AuthServicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Controller
public class HistorialControlador {

    private final Logger logger = LoggerFactory.getLogger(HistorialControlador.class);

    private final CitaServicio citaServicio;
    private final AuthServicio authServicio;

    public HistorialControlador(CitaServicio citaServicio, AuthServicio authServicio) {
        this.citaServicio = citaServicio;
        this.authServicio = authServicio;
    }

    private Paciente getPacienteLogueado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() instanceof String) {
            return null;
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String correoUsuario = userDetails.getUsername();

        Optional<Paciente> pacienteOpt = authServicio.buscarPorCorreo(correoUsuario);
        return pacienteOpt.orElse(null);
    }

    @GetMapping("/historial")
    public String mostrarPaginaHistorialCitas(Model model) {
        logger.info("El usuario ha accedido a la página de historial de citas.");

        Paciente usuario = getPacienteLogueado();
        if (usuario == null || usuario.getId() == null) {
            logger.warn("Intento de acceder a historial sin paciente logueado o con ID de paciente nulo.");
            return "redirect:/login?error=nologin";
        }

        try {
            List<Cita> citasPendientes = citaServicio.obtenerCitasPendientesPorPaciente(usuario.getId());
            List<Cita> historialCitas = citaServicio.obtenerHistorialCitasPorPaciente(usuario.getId());

            model.addAttribute("citasPendientes", citasPendientes);
            model.addAttribute("historialCitas", historialCitas);
            model.addAttribute("nombreUsuario", usuario.getNombre() + " " + usuario.getApellido());

        } catch (IllegalArgumentException e) {
            logger.error("Error al obtener historial de citas para paciente ID {}: {}", usuario.getId(), e.getMessage());
            model.addAttribute("error", "Error al cargar sus citas: " + e.getMessage());
            model.addAttribute("citasPendientes", List.of());
            model.addAttribute("historialCitas", List.of());
        } catch (Exception e) {
            logger.error("Error inesperado al cargar el historial de citas para paciente ID {}: {}", usuario.getId(), e.getMessage(), e);
            model.addAttribute("error", "Ocurrió un error inesperado al cargar sus citas.");
            model.addAttribute("citasPendientes", List.of());
            model.addAttribute("historialCitas", List.of());
        }

        return "historialCita";
    }

    @GetMapping("/historial/exportar/excel")
    public ResponseEntity<byte[]> exportarHistorialCitasExcel() {
        Paciente pacienteLogueado = getPacienteLogueado();
        if (pacienteLogueado == null || pacienteLogueado.getId() == null) {
            logger.warn("Intento de exportar historial de citas sin paciente logueado o ID nulo.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autorizado: Debes iniciar sesión para exportar.".getBytes());
        }

        try {
            List<Cita> citasDelPaciente = citaServicio.obtenerCitasPendientesPorPaciente(pacienteLogueado.getId());
            citasDelPaciente.addAll(citaServicio.obtenerHistorialCitasPorPaciente(pacienteLogueado.getId()));

            try (Workbook workbook = new XSSFWorkbook();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                Sheet sheet = workbook.createSheet(
                                 "Historial Citas - " + pacienteLogueado.getNombre() + " " + pacienteLogueado.getApellido());

                Row headerRow = sheet.createRow(0);
                String[] headers = { "ID Cita", "Fecha", "Hora", "Estado", "Médico", "Especialidad" };
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                }

                int rowNum = 1;
                for (Cita cita : citasDelPaciente) {
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
                        row.createCell(4).setCellValue("Médico Desconocido");
                        row.createCell(5).setCellValue("N/A");
                    }
                }

                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }

                workbook.write(outputStream);
                logger.info("Reporte Excel de citas generado para paciente ID: {}", pacienteLogueado.getId());

                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                httpHeaders.setContentDispositionFormData("attachment",
                        "historial_citas_" + pacienteLogueado.getId() + ".xlsx");
                httpHeaders.setContentLength(outputStream.toByteArray().length);

                return ResponseEntity.ok().headers(httpHeaders).body(outputStream.toByteArray());

            } catch (IOException e) {
                logger.error("Error IO al generar el reporte de citas en Excel para paciente ID {}: {}",
                        pacienteLogueado.getId(), e.getMessage(), e);
                return ResponseEntity.internalServerError()
                        .body(("Error al generar el reporte: " + e.getMessage()).getBytes());
            }

        } catch (IllegalArgumentException e) {
            logger.error("Error de argumento (ej. paciente no encontrado) al generar el reporte de citas en Excel para paciente ID {}: {}",
                    pacienteLogueado.getId(), e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage().getBytes());
        } catch (Exception e) {
            logger.error("Error inesperado al generar el reporte de citas en Excel para paciente ID {}: {}",
                    pacienteLogueado.getId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body(("Error inesperado al generar el reporte: " + e.getMessage()).getBytes());
        }
    }

    @PostMapping("/cancelar-cita")
    @ResponseBody
    public ResponseEntity<String> cancelarCita(@RequestParam Long id) {
        try {
            boolean exito = citaServicio.cancelarCita(id);
            if (exito) {
                logger.info("Cita ID {} cancelada exitosamente por solicitud del controlador.", id);
                return ResponseEntity.ok("Cita cancelada correctamente.");
            } else {
                logger.warn("Intento de cancelar cita ID {} fallido: Cita no encontrada.", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No se encontró la cita para cancelar.");
            }
        } catch (IllegalArgumentException e) {
            logger.error("Error de argumento al intentar cancelar cita ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body("Error al cancelar la cita: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado al cancelar cita ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno al cancelar la cita.");
        }
    }
}

