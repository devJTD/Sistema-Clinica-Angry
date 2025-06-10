package com.clinica.sistema.Controlador;

import com.clinica.sistema.Modelo.Cita;
import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Modelo.Medico;
import com.clinica.sistema.Servicio.CitaServicio;

import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.io.ByteArrayOutputStream;

import org.springframework.http.HttpStatus;

@Controller
public class HistorialControlador {

    private Logger logger = LoggerFactory.getLogger(HistorialControlador.class);
    private CitaServicio citaServicio = new CitaServicio();

    public HistorialControlador(CitaServicio citaServicio) {
        this.citaServicio = citaServicio;
    }

    @GetMapping("/historial")
    public String mostrarPaginaHistorialCitas(HttpSession session, Model model) {
        logger.info("El usuario ha accedido a la página de historial de citas.");

        try {
            Paciente usuario = (Paciente) session.getAttribute("usuario");
            if (usuario == null) {
                return "redirect:/login";
            }

            // Obtener solo citas del paciente logueado
            List<Cita> citasUsuario = citaServicio.obtenerCitasPorPaciente(usuario.getId());

            model.addAttribute("citasPendientes", citasUsuario);
            model.addAttribute("nombreUsuario", usuario.getNombre());

        } catch (IOException e) {
            logger.error("Error al leer citas", e);
            model.addAttribute("citasPendientes", List.of());
        }

        return "historialCita";
    }

    @GetMapping("/historial/exportar/excel")
    public ResponseEntity<byte[]> exportarHistorialCitasExcel(HttpSession session) {
        Paciente pacienteLogueado = (Paciente) session.getAttribute("usuario");
        if (pacienteLogueado == null) {
            logger.warn("Intento de exportar historial de citas sin paciente logueado.");
            return ResponseEntity.status(401).body("No autorizado: Debes iniciar sesión para exportar.".getBytes());
        }

        try {
            List<Cita> citasDelPaciente = citaServicio.leerCitas().stream()
                    .filter(cita -> cita.getIdPaciente().equals(pacienteLogueado.getId()))
                    .collect(Collectors.toList());

            List<Medico> todosLosMedicos = citaServicio.obtenerTodosLosMedicos();

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

                    Medico medicoAsociado = todosLosMedicos.stream()
                            .filter(m -> m.getId().equals(cita.getIdMedico()))
                            .findFirst()
                            .orElse(null);

                    if (medicoAsociado != null) {

                        row.createCell(4).setCellValue(medicoAsociado.getNombreCompleto()); 
                                                                                            
                        row.createCell(5).setCellValue(medicoAsociado.getIdEspecialidad());
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

        } catch (IOException e) {
            logger.error("Error al obtener datos (JSON) para el reporte de citas en Excel para paciente ID {}: {}",
                    pacienteLogueado.getId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body(("Error al obtener datos: " + e.getMessage()).getBytes());
        } catch (IllegalArgumentException e) {
            logger.error("Error de argumento al generar el reporte: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage().getBytes());
        }
    }

     // Nueva funcionalidad: cancelar cita (cambiar estado a "cancelada")
    @PostMapping("/cancelar-cita")
    @ResponseBody
    public ResponseEntity<String> cancelarCita(@RequestParam Long id) {
        try {
            boolean exito = citaServicio.cancelarCita(id);
            if (exito) {
                return ResponseEntity.ok("Cita cancelada");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No se encontró la cita");
            }
        } catch (Exception e) {
            logger.error("Error al cancelar cita", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al cancelar la cita");
        }
    }
}

