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

@Controller // Indica que esta clase es un controlador Spring MVC.
public class AmbulanciaControlador {

    private final Logger logger = LoggerFactory.getLogger(AmbulanciaControlador.class); // Logger para registrar eventos.

    private final AuthServicio authServicio; // Servicio para la lógica de autenticación.
    private final DireccionServicio direccionServicio; // Servicio para la gestión de direcciones.

    // Constructor que inyecta los servicios necesarios.
    public AmbulanciaControlador(AuthServicio authServicio, DireccionServicio direccionServicio) {
        this.authServicio = authServicio;
        this.direccionServicio = direccionServicio;
    }

    // Método auxiliar para obtener el objeto Paciente del usuario actualmente logueado.
    private Paciente getPacienteLogueado() {
        // Obtiene la información de autenticación del contexto de seguridad de Spring.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Verifica si el usuario está autenticado y si el principal es un objeto UserDetails.
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() instanceof String) {
            logger.debug("No hay usuario autenticado o el principal no es un UserDetails.");
            return null;
        }

        // Extrae los detalles del usuario y su correo electrónico (usado como nombre de usuario).
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

    // Muestra la página de solicitud de ambulancia y carga las direcciones asociadas al paciente logueado.
    @GetMapping("/ambulancia")
    public String mostrarPaginaAmbulancia(Model model) {
        logger.info("El usuario ha accedido a la pagina de solicitud de ambulancia.");
        // Obtiene el paciente logueado.
        Paciente pacienteLogueado = getPacienteLogueado();
        // Si no hay un paciente logueado o su ID es nulo, redirige a la página de login.
        if (pacienteLogueado == null || pacienteLogueado.getId() == null) {
            logger.warn("Usuario no logueado intento acceder a la pagina de ambulancia. Redirigiendo a login.");
            return "redirect:/login?error=nologin";
        }

        // Añade el nombre completo del paciente al modelo.
        model.addAttribute("nombreCompleto", pacienteLogueado.getNombre() + " " + pacienteLogueado.getApellido());
        logger.info("Usuario con DNI: {} ({}) ha accedido a la pagina de solicitud de ambulancia.", pacienteLogueado.getDni(), pacienteLogueado.getCorreo());

        try {
            // Obtiene la lista de direcciones del paciente.
            List<Direccion> direcciones = direccionServicio.obtenerDireccionesPorPaciente(pacienteLogueado.getId());
            // Añade las direcciones al modelo.
            model.addAttribute("direccionesPaciente", direcciones);
            logger.info("Se cargaron {} direcciones para el usuario con DNI: {}.", direcciones.size(), pacienteLogueado.getDni());
        } catch (Exception e) {
            // Maneja cualquier error al cargar las direcciones.
            logger.error("Error al cargar las direcciones del usuario con DNI: {}: {}", pacienteLogueado.getDni(), e.getMessage(), e);
            model.addAttribute("direccionesPaciente", Collections.emptyList()); // Añade una lista vacía al modelo.
            model.addAttribute("error", "Error al cargar sus direcciones."); // Añade un mensaje de error al modelo.
        }

        return "ambulancia"; // Retorna el nombre de la vista (ambulancia.html).
    }

    // Guarda una nueva dirección para el paciente logueado.
    // Este método es un endpoint REST que retorna JSON.
    @PostMapping("/paciente/direcciones")
    @ResponseBody // Indica que la respuesta de este método debe ser directamente el cuerpo de la respuesta HTTP.
    public ResponseEntity<?> guardarNuevaDireccion(@RequestBody Map<String, String> payload) {
        // Obtiene el paciente logueado.
        Paciente pacienteLogueado = getPacienteLogueado();
        // Si no está logueado, retorna un error de no autorizado.
        if (pacienteLogueado == null || pacienteLogueado.getId() == null) {
            logger.warn("Intento de guardar nueva direccion por usuario no logueado o sin ID de paciente. Retornando no autorizado.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "No autorizado."));
        }

        // Extrae la dirección completa del cuerpo de la solicitud (JSON).
        String direccionCompleta = payload.get("direccionCompleta");
        logger.info("Usuario con DNI: {} ({}) intentando guardar nueva direccion. Datos recibidos: [Direccion Completa: {}]", pacienteLogueado.getDni(), pacienteLogueado.getCorreo(), direccionCompleta);

        // Valida que la dirección no esté vacía.
        if (direccionCompleta == null || direccionCompleta.trim().isEmpty()) {
            logger.warn("Intento fallido de guardar direccion: La direccion no puede estar vacia para el usuario con DNI: {}.", pacienteLogueado.getDni());
            return ResponseEntity.badRequest().body(Map.of("message", "La direccion no puede estar vacia."));
        }

        try {
            // Crea un nuevo objeto Direccion y lo asocia al paciente.
            Direccion nuevaDireccion = new Direccion();
            nuevaDireccion.setDireccionCompleta(direccionCompleta);
            nuevaDireccion.setPaciente(pacienteLogueado);

            // Guarda la nueva dirección usando el servicio.
            Direccion direccionGuardada = direccionServicio.guardarDireccion(nuevaDireccion);
            logger.info("Nueva direccion con ID: {} guardada exitosamente para el usuario con DNI: {}. Direccion: {}", direccionGuardada.getId(), pacienteLogueado.getDni(), direccionGuardada.getDireccionCompleta());
            return ResponseEntity.ok(direccionGuardada); // Retorna la dirección guardada con estado HTTP 200 OK.
        } catch (Exception e) {
            // Maneja cualquier error interno al guardar la dirección.
            logger.error("Error interno al guardar la nueva direccion para el usuario con DNI: {}: {}", pacienteLogueado.getDni(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error interno al guardar la direccion."));
        }
    }

    // Procesa la solicitud de ambulancia para el paciente logueado.
    // Este método es un endpoint REST que retorna JSON.
    @PostMapping("/solicitar-ambulancia")
    @ResponseBody // Indica que la respuesta de este método debe ser directamente el cuerpo de la respuesta HTTP.
    public ResponseEntity<?> solicitarAmbulancia(@RequestBody Map<String, Long> payload) {
        // Obtiene el paciente logueado.
        Paciente pacienteLogueado = getPacienteLogueado();
        // Si no está logueado, retorna un error de no autorizado.
        if (pacienteLogueado == null || pacienteLogueado.getId() == null) {
            logger.warn("Intento de solicitar ambulancia por usuario no logueado o sin ID de paciente. Retornando no autorizado.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "No autorizado."));
        }

        // Extrae el ID de la dirección seleccionada del cuerpo de la solicitud (JSON).
        Long direccionId = payload.get("direccionId");
        logger.info("Usuario con DNI: {} ({}) intentando solicitar ambulancia. Datos recibidos: [ID Direccion Seleccionada: {}]", pacienteLogueado.getDni(), pacienteLogueado.getCorreo(), direccionId);

        // Valida que se haya proporcionado un ID de dirección.
        if (direccionId == null) {
            logger.warn("Intento fallido de solicitar ambulancia: Debe proporcionar una ID de direccion para el usuario con DNI: {}.", pacienteLogueado.getDni());
            return ResponseEntity.badRequest().body(Map.of("message", "Debe proporcionar una ID de direccion."));
        }

        try {
            // Busca la dirección por ID y verifica que pertenezca al paciente logueado.
            Optional<Direccion> direccionOpt = direccionServicio.buscarPorIdYPacienteId(direccionId, pacienteLogueado.getId());
            if (direccionOpt.isEmpty()) {
                logger.warn("Intento fallido de solicitar ambulancia: La direccion con ID: {} no es valida o no pertenece al usuario con DNI: {}.", direccionId, pacienteLogueado.getDni());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "La direccion seleccionada no es valida o no le pertenece."));
            }

            Direccion direccionSolicitada = direccionOpt.get();
            // Aquí iría la lógica para "solicitar" la ambulancia, por ejemplo:
            // - Enviar una notificación a un sistema de gestión de ambulancias.
            // - Guardar un registro de la solicitud en la base de datos (Ej: en una tabla de 'SolicitudAmbulancia').
            // - Actualizar el estado de la dirección o del paciente si fuera necesario.

            logger.info("Solicitud de ambulancia procesada exitosamente para el usuario con DNI: {} con direccion: {} (ID: {}).", pacienteLogueado.getDni(), direccionSolicitada.getDireccionCompleta(), direccionId);
            return ResponseEntity.ok(Map.of("message", "Solicitud de ambulancia procesada exitosamente.")); // Retorna un mensaje de éxito con estado HTTP 200 OK.

        } catch (Exception e) {
            // Maneja cualquier error interno al procesar la solicitud de ambulancia.
            logger.error("Error interno al procesar la solicitud de ambulancia para el usuario con DNI: {}: {}", pacienteLogueado.getDni(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error interno al procesar la solicitud de ambulancia."));
        }
    }
}