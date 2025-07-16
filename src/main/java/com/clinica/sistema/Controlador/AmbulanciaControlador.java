package com.clinica.sistema.Controlador;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.clinica.sistema.Modelo.Direccion;
import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Servicio.AuthServicio;
import com.clinica.sistema.Servicio.DireccionServicio;

@Controller
public class AmbulanciaControlador {

    private final Logger logger = LoggerFactory.getLogger(AmbulanciaControlador.class);

    private final AuthServicio authServicio;
    private final DireccionServicio direccionServicio;

    public AmbulanciaControlador(AuthServicio authServicio, DireccionServicio direccionServicio) {
        this.authServicio = authServicio;
        this.direccionServicio = direccionServicio;
    }

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
            logger.debug("Paciente logueado encontrado con ID: {} y correo: {}", pacienteOpt.get().getId(), correoUsuario);
        } else {
            logger.warn("No se encontro paciente en la base de datos para el correo: {}", correoUsuario);
        }
        return pacienteOpt.orElse(null);
    }

    @GetMapping("/ambulancia")
    public String mostrarPaginaAmbulancia(Model model) {
        logger.info("El usuario ha accedido a la pagina de solicitud de ambulancia.");
        Paciente pacienteLogueado = getPacienteLogueado();
        if (pacienteLogueado == null || pacienteLogueado.getId() == null) {
            logger.warn("Usuario no logueado intento acceder a la pagina de ambulancia. Redirigiendo a login.");
            return "redirect:/login?error=nologin";
        }

        model.addAttribute("nombreCompleto", pacienteLogueado.getNombre() + " " + pacienteLogueado.getApellido());
        logger.info("Usuario con DNI: {} ({}) ha accedido a la pagina de solicitud de ambulancia.", pacienteLogueado.getDni(), pacienteLogueado.getCorreo());

        try {
            List<Direccion> direcciones = direccionServicio.obtenerDireccionesPorPaciente(pacienteLogueado.getId());
            model.addAttribute("direccionesPaciente", direcciones);
            logger.info("Se cargaron {} direcciones para el usuario con DNI: {}.", direcciones.size(), pacienteLogueado.getDni());
        } catch (Exception e) {
            logger.error("Error al cargar las direcciones del usuario con DNI: {}: {}", pacienteLogueado.getDni(), e.getMessage(), e);
            model.addAttribute("direccionesPaciente", Collections.emptyList());
            model.addAttribute("error", "Error al cargar sus direcciones.");
        }

        return "ambulancia";
    }

    @PostMapping("/paciente/direcciones")
    @ResponseBody
    public ResponseEntity<?> guardarNuevaDireccion(@RequestBody Map<String, String> payload) {
        Paciente pacienteLogueado = getPacienteLogueado();
        if (pacienteLogueado == null || pacienteLogueado.getId() == null) {
            logger.warn("Intento de guardar nueva direccion por usuario no logueado o sin ID de paciente. Retornando no autorizado.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "No autorizado."));
        }

        String direccionCompleta = payload.get("direccionCompleta");
        logger.info("Usuario con DNI: {} ({}) intentando guardar nueva direccion. Datos recibidos: [Direccion Completa: {}]", pacienteLogueado.getDni(), pacienteLogueado.getCorreo(), direccionCompleta);

        if (direccionCompleta == null || direccionCompleta.trim().isEmpty()) {
            logger.warn("Intento fallido de guardar direccion: La direccion no puede estar vacia para el usuario con DNI: {}.", pacienteLogueado.getDni());
            return ResponseEntity.badRequest().body(Map.of("message", "La direccion no puede estar vacia."));
        }

        try {
            Direccion nuevaDireccion = new Direccion();
            nuevaDireccion.setDireccionCompleta(direccionCompleta);
            nuevaDireccion.setPaciente(pacienteLogueado);

            Direccion direccionGuardada = direccionServicio.guardarDireccion(nuevaDireccion);
            logger.info("Nueva direccion con ID: {} guardada exitosamente para el usuario con DNI: {}. Direccion: {}", direccionGuardada.getId(), pacienteLogueado.getDni(), direccionGuardada.getDireccionCompleta());
            return ResponseEntity.ok(direccionGuardada);
        } catch (Exception e) {
            logger.error("Error interno al guardar la nueva direccion para el usuario con DNI: {}: {}", pacienteLogueado.getDni(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error interno al guardar la direccion."));
        }
    }

    @PostMapping("/solicitar-ambulancia")
    @ResponseBody
    public ResponseEntity<?> solicitarAmbulancia(@RequestBody Map<String, Long> payload) {
        Paciente pacienteLogueado = getPacienteLogueado();
        if (pacienteLogueado == null || pacienteLogueado.getId() == null) {
            logger.warn("Intento de solicitar ambulancia por usuario no logueado o sin ID de paciente. Retornando no autorizado.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "No autorizado."));
        }

        Long direccionId = payload.get("direccionId");
        logger.info("Usuario con DNI: {} ({}) intentando solicitar ambulancia. Datos recibidos: [ID Direccion Seleccionada: {}]", pacienteLogueado.getDni(), pacienteLogueado.getCorreo(), direccionId);

        if (direccionId == null) {
            logger.warn("Intento fallido de solicitar ambulancia: Debe proporcionar una ID de direccion para el usuario con DNI: {}.", pacienteLogueado.getDni());
            return ResponseEntity.badRequest().body(Map.of("message", "Debe proporcionar una ID de direccion."));
        }

        try {
            Optional<Direccion> direccionOpt = direccionServicio.buscarPorIdYPacienteId(direccionId, pacienteLogueado.getId());
            if (direccionOpt.isEmpty()) {
                logger.warn("Intento fallido de solicitar ambulancia: La direccion con ID: {} no es valida o no pertenece al usuario con DNI: {}.", direccionId, pacienteLogueado.getDni());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "La direccion seleccionada no es valida o no le pertenece."));
            }

            Direccion direccionSolicitada = direccionOpt.get();
            logger.info("Solicitud de ambulancia procesada exitosamente para el usuario con DNI: {} con direccion: {} (ID: {}).", pacienteLogueado.getDni(), direccionSolicitada.getDireccionCompleta(), direccionId);
            return ResponseEntity.ok(Map.of("message", "Solicitud de ambulancia procesada exitosamente."));

        } catch (Exception e) {
            logger.error("Error interno al procesar la solicitud de ambulancia para el usuario con DNI: {}: {}", pacienteLogueado.getDni(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error interno al procesar la solicitud de ambulancia."));
        }
    }
}