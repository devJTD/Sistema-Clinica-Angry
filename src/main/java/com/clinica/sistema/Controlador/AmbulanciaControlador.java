package com.clinica.sistema.Controlador;

import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Modelo.Direccion; // Asegúrate de importar tu modelo Direccion
import com.clinica.sistema.Servicio.AuthServicio; // O tu PacienteServicio
import com.clinica.sistema.Servicio.DireccionServicio; // Si tienes un servicio específico para Direcciones

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class AmbulanciaControlador {

    private final Logger logger = LoggerFactory.getLogger(AmbulanciaControlador.class);

    private final AuthServicio authServicio;
    private final DireccionServicio direccionServicio; // Asumiendo que tienes un servicio para manejar Direcciones

    // Inyecta tus servicios
    public AmbulanciaControlador(AuthServicio authServicio, DireccionServicio direccionServicio) {
        this.authServicio = authServicio;
        this.direccionServicio = direccionServicio; // Inyecta el servicio de direcciones
    }

    /**
     * Método auxiliar para obtener el paciente actualmente logueado.
     * Reutilizado de la lógica del HistorialControlador.
     */
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

    @GetMapping("/ambulancia")
    public String mostrarPaginaAmbulancia(Model model) {
        logger.info("Accediendo a la página de ambulancia.");

        Paciente pacienteLogueado = getPacienteLogueado();
        if (pacienteLogueado == null || pacienteLogueado.getId() == null) {
            logger.warn("Intento de acceder a la página de ambulancia sin paciente logueado o con ID de paciente nulo. Redirigiendo a login.");
            return "redirect:/login?error=nologin";
        }

        model.addAttribute("nombreCompleto", pacienteLogueado.getNombre() + " " + pacienteLogueado.getApellido());

        // Cargar las direcciones del paciente
        try {
            // Es crucial que tu Paciente (o un servicio) pueda cargar las direcciones
            // Si la relación @OneToMany en Paciente es LAZY, necesitarás un método en tu servicio
            // para cargar explícitamente las direcciones, o asegurarte de que tu AuthServicio.buscarPorCorreo
            // las cargue (ej. usando un JOIN FETCH en el repositorio o marcando el método @Transactional si se acceden)
            List<Direccion> direcciones = direccionServicio.obtenerDireccionesPorPaciente(pacienteLogueado.getId());
            model.addAttribute("direccionesPaciente", direcciones);
            logger.debug("Cargadas {} direcciones para el paciente {}.", direcciones.size(), pacienteLogueado.getCorreo());
        } catch (Exception e) {
            logger.error("Error al cargar las direcciones para el paciente {}: {}", pacienteLogueado.getCorreo(), e.getMessage(), e);
            model.addAttribute("direccionesPaciente", Collections.emptyList()); // Asegura que no sea null
            model.addAttribute("error", "Error al cargar sus direcciones.");
        }

        return "ambulancia";
    }

    // Nuevo endpoint para guardar una nueva dirección desde el modal
    @PostMapping("/paciente/direcciones")
    @ResponseBody // Indica que el retorno es JSON/XML, no una vista
    public ResponseEntity<?> guardarNuevaDireccion(@RequestBody Map<String, String> payload) {
        Paciente pacienteLogueado = getPacienteLogueado();
        if (pacienteLogueado == null || pacienteLogueado.getId() == null) {
            logger.warn("Intento de guardar dirección sin paciente logueado.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "No autorizado."));
        }

        String direccionCompleta = payload.get("direccionCompleta");
        if (direccionCompleta == null || direccionCompleta.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "La dirección no puede estar vacía."));
        }

        try {
            Direccion nuevaDireccion = new Direccion();
            nuevaDireccion.setDireccionCompleta(direccionCompleta);
            nuevaDireccion.setPaciente(pacienteLogueado); // Asocia la dirección al paciente

            Direccion direccionGuardada = direccionServicio.guardarDireccion(nuevaDireccion);
            logger.info("Nueva dirección guardada para el paciente {}: {}", pacienteLogueado.getCorreo(), direccionCompleta);
            return ResponseEntity.ok(direccionGuardada); // Retorna la dirección guardada (incluyendo su ID)
        } catch (Exception e) {
            logger.error("Error al guardar la nueva dirección para el paciente {}: {}", pacienteLogueado.getCorreo(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error interno al guardar la dirección."));
        }
    }


    // Nuevo endpoint para procesar la solicitud de ambulancia
    @PostMapping("/solicitar-ambulancia")
    @ResponseBody // Indica que el retorno es JSON/XML, no una vista
    public ResponseEntity<?> solicitarAmbulancia(@RequestBody Map<String, Long> payload) {
        Paciente pacienteLogueado = getPacienteLogueado();
        if (pacienteLogueado == null || pacienteLogueado.getId() == null) {
            logger.warn("Intento de solicitar ambulancia sin paciente logueado.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "No autorizado."));
        }

        Long direccionId = payload.get("direccionId");
        if (direccionId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Debe proporcionar una ID de dirección."));
        }

        try {
            // Aquí simularías la lógica de enviar la solicitud de ambulancia
            // 1. Verificar si la dirección pertenece al paciente logueado
            Optional<Direccion> direccionOpt = direccionServicio.buscarPorIdYPacienteId(direccionId, pacienteLogueado.getId());
            if (direccionOpt.isEmpty()) {
                logger.warn("Intento de solicitar ambulancia para una dirección no asociada al paciente logueado. Direccion ID: {}", direccionId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "La dirección seleccionada no es válida o no le pertenece."));
            }

            Direccion direccionSolicitada = direccionOpt.get();
            logger.info("Solicitud de ambulancia recibida para paciente {} en la dirección: {}", pacienteLogueado.getCorreo(), direccionSolicitada.getDireccionCompleta());


            // Simulación de éxito
            return ResponseEntity.ok(Map.of("message", "Solicitud de ambulancia procesada exitosamente."));

        } catch (Exception e) {
            logger.error("Error al procesar la solicitud de ambulancia para paciente {}: {}", pacienteLogueado.getCorreo(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error interno al procesar la solicitud de ambulancia."));
        }
    }
}