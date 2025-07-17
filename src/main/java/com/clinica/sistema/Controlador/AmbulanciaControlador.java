package com.clinica.sistema.Controlador;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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

    private final Logger logger = LoggerFactory.getLogger(AmbulanciaControlador.class); // Logger para registrar eventos.

    private final AuthServicio authServicio; // Servicio para la logica de autenticacion.
    private final DireccionServicio direccionServicio; // Servicio para la gestion de direcciones.

    // Constantes para las claves MDC
    private static final String MDC_USER_FULL_NAME = "userFullName";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_USER_DNI = "userDni";

    // Constructor que inyecta los servicios necesarios.
    public AmbulanciaControlador(AuthServicio authServicio, DireccionServicio direccionServicio) {
        this.authServicio = authServicio;
        this.direccionServicio = direccionServicio;
    }

    // Metodo auxiliar para obtener el objeto Paciente del usuario actualmente logueado y establecer el MDC.
    // Este metodo AHORA SOLO ESTABLECE el MDC. La limpieza se hara en el 'finally' de cada metodo de controlador.
    private Paciente getPacienteLogueado() {
        // Obtiene la informacion de autenticacion del contexto de seguridad de Spring.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Verifica si el usuario esta autenticado y si el principal es un objeto UserDetails.
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() instanceof String) {
            logger.debug("No hay usuario autenticado o el principal no es un UserDetails.");
            // No limpiar MDC aqui, ya que este metodo es llamado por otros que pueden haberlo establecido.
            return null;
        }

        // Extrae los detalles del usuario y su correo electronico (usado como nombre de usuario).
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String correoUsuario = userDetails.getUsername();
        logger.debug("Buscando paciente logueado con correo: {}", correoUsuario);

        // Busca el paciente en la base de datos usando el correo.
        Optional<Paciente> pacienteOpt = authServicio.buscarPorCorreo(correoUsuario);
        if (pacienteOpt.isPresent()) {
            Paciente paciente = pacienteOpt.get();
            // Establece los valores en el MDC
            MDC.put(MDC_USER_FULL_NAME, paciente.getNombre() + " " + paciente.getApellido());
            MDC.put(MDC_USER_ID, String.valueOf(paciente.getId()));
            MDC.put(MDC_USER_DNI, paciente.getDni());
            logger.debug("Paciente logueado encontrado con ID: {} y correo: {}. MDC establecido.", paciente.getId(), correoUsuario);
        } else {
            logger.warn("No se encontro paciente en la base de datos para el correo: {}.", correoUsuario);
            // No limpiar MDC aqui, dejar que el 'finally' del controlador lo haga.
        }
        return pacienteOpt.orElse(null);
    }

    // Metodo auxiliar para limpiar informacion del paciente del MDC
    private void clearPacienteMDCContext() {
        MDC.remove(MDC_USER_FULL_NAME);
        MDC.remove(MDC_USER_ID);
        MDC.remove(MDC_USER_DNI);
    }


    @GetMapping("/ambulancia")
    public String mostrarPaginaAmbulancia(Model model) {
        // Primero, intentar obtener el paciente logueado.
        // getPacienteLogueado() AHORA es responsable de establecer el MDC.
        Paciente pacienteLogueado = getPacienteLogueado();

        try {
            // El primer log ahora usa el MDC, si esta disponible.
            // Siempre se logueara algo, ya sea con info del paciente o como no autenticado.
            if (MDC.get(MDC_USER_FULL_NAME) != null) {
                logger.info("El paciente {} (ID: {}, DNI: {}) ha accedido a la pagina de solicitud de ambulancia.",
                            MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI));
            } else {
                logger.info("Un usuario no autenticado ha accedido a la pagina de solicitud de ambulancia.");
            }

            // Si no hay un paciente logueado o su ID es nulo, redirige a la pagina de login.
            if (pacienteLogueado == null || pacienteLogueado.getId() == null) {
                logger.warn("Redirigiendo a login: Usuario no logueado o sin ID intento acceder a la pagina de ambulancia.");
                return "redirect:/login?error=nologin";
            }

            // Añade el nombre completo del paciente al modelo.
            model.addAttribute("nombreCompleto", pacienteLogueado.getNombre() + " " + pacienteLogueado.getApellido());

     
            // Aqui esta el bloque original que causaba el problema del MDC null.
            // Ahora todo el manejo de direcciones y su log se mueve fuera del 'try' anidado
            // y dentro del 'try' principal para que el 'finally' principal lo cubra.
            try {
                // Obtiene la lista de direcciones del paciente.
                List<Direccion> direcciones = direccionServicio.obtenerDireccionesPorPaciente(pacienteLogueado.getId());
                // Añade las direcciones al modelo.
                model.addAttribute("direccionesPaciente", direcciones);
                // ESTA LINEA DE LOG AHORA ESTA DENTRO DEL TRY EXTERNO Y ANTES DEL FINALLY EXTERNO.
                logger.info("El paciente {} (ID: {}, DNI: {}) ha cargado {} direcciones.",
                            MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI), direcciones.size());
            } catch (IllegalArgumentException e) {
                // Maneja cualquier error al cargar las direcciones.
                logger.error("Error al cargar las direcciones del paciente {} (ID: {}, DNI: {}): {}",
                            MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI), e.getMessage(), e);
                model.addAttribute("direccionesPaciente", Collections.emptyList()); // Añade una lista vacia al modelo.
                model.addAttribute("error", "Error al cargar sus direcciones."); // Añade un mensaje de error al modelo.
            }

            return "ambulancia"; // Retorna el nombre de la vista (ambulancia.html).

        } catch (IllegalArgumentException e) { // Captura cualquier otra excepcion que pueda ocurrir antes del finally
            logger.error("Ocurrio un error inesperado al mostrar la pagina de ambulancia para el paciente {} (ID: {}, DNI: {}): {}",
                        MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI), e.getMessage(), e);
            model.addAttribute("error", "Ocurrio un error inesperado. Por favor, intente de nuevo.");
            return "errorPage"; // O una pagina de error generica
        } finally {
            clearPacienteMDCContext(); // Limpiar MDC al finalizar el metodo del controlador
        }
    }


    @PostMapping("/paciente/direcciones")
    @ResponseBody
    public ResponseEntity<?> guardarNuevaDireccion(@RequestBody Map<String, String> payload) {
        Paciente pacienteLogueado = getPacienteLogueado(); // Esto establecera el MDC si el paciente es encontrado

        try {
            // Si no esta logueado, retorna un error de no autorizado.
            if (pacienteLogueado == null || pacienteLogueado.getId() == null) {
                logger.warn("Intento de guardar nueva direccion por usuario no logueado o sin ID de paciente. Retornando no autorizado.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "No autorizado."));
            }

            // Extrae la direccion completa del cuerpo de la solicitud (JSON).
            String direccionCompleta = payload.get("direccionCompleta");
            logger.info("El paciente {} (ID: {}, DNI: {}) esta intentando guardar una nueva direccion. Datos recibidos: [Direccion Completa: {}]",
                        MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI), direccionCompleta);

            // Valida que la direccion no este vacia.
            if (direccionCompleta == null || direccionCompleta.trim().isEmpty()) {
                logger.warn("Intento fallido de guardar direccion: La direccion no puede estar vacia para el paciente {} (ID: {}, DNI: {}).",
                            MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI));
                return ResponseEntity.badRequest().body(Map.of("message", "La direccion no puede estar vacia."));
            }

            // Crea un nuevo objeto Direccion y lo asocia al paciente.
            Direccion nuevaDireccion = new Direccion();
            nuevaDireccion.setDireccionCompleta(direccionCompleta);
            nuevaDireccion.setPaciente(pacienteLogueado);

            // Guarda la nueva direccion usando el servicio.
            Direccion direccionGuardada = direccionServicio.guardarDireccion(nuevaDireccion);
            logger.info("El paciente {} (ID: {}, DNI: {}) ha guardado una nueva direccion con ID: {}. Direccion: {}",
                        MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI), direccionGuardada.getId(), direccionGuardada.getDireccionCompleta());
            return ResponseEntity.ok(direccionGuardada); // Retorna la direccion guardada con estado HTTP 200 OK.
        } catch (IllegalArgumentException e) {
            // Maneja cualquier error interno al guardar la direccion.
            logger.error("Error interno al guardar la nueva direccion para el paciente {} (ID: {}, DNI: {}): {}",
                        MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error interno al guardar la direccion."));
        } finally {
            clearPacienteMDCContext(); // Limpiar MDC al finalizar el metodo del controlador
        }
    }


    @PostMapping("/solicitar-ambulancia")
    @ResponseBody // Indica que la respuesta de este metodo debe ser directamente el cuerpo de la respuesta HTTP.
    public ResponseEntity<?> solicitarAmbulancia(@RequestBody Map<String, Long> payload) {
        Paciente pacienteLogueado = getPacienteLogueado(); // Esto establecera el MDC si el paciente es encontrado

        try {
            // Si no esta logueado, retorna un error de no autorizado.
            if (pacienteLogueado == null || pacienteLogueado.getId() == null) {
                logger.warn("Intento de solicitar ambulancia por usuario no logueado o sin ID de paciente. Retornando no autorizado.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "No autorizado."));
            }

            // Extrae el ID de la direccion seleccionada del cuerpo de la solicitud (JSON).
            Long direccionId = payload.get("direccionId");
            logger.info("El paciente {} (ID: {}, DNI: {}) esta intentando solicitar una ambulancia. Datos recibidos: [ID Direccion Seleccionada: {}]",
                        MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI), direccionId);

            // Valida que se haya proporcionado un ID de direccion.
            if (direccionId == null) {
                logger.warn("Intento fallido de solicitar ambulancia: Debe proporcionar una ID de direccion para el paciente {} (ID: {}, DNI: {}).",
                            MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI));
                return ResponseEntity.badRequest().body(Map.of("message", "Debe proporcionar una ID de direccion."));
            }

            // Busca la direccion por ID y verifica que pertenezca al paciente logueado.
            Optional<Direccion> direccionOpt = direccionServicio.buscarPorIdYPacienteId(direccionId, pacienteLogueado.getId());
            if (direccionOpt.isEmpty()) {
                logger.warn("Intento fallido de solicitar ambulancia: La direccion con ID: {} no es valida o no pertenece al paciente {} (ID: {}, DNI: {}).",
                            direccionId, MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "La direccion seleccionada no es valida o no le pertenece."));
            }

            Direccion direccionSolicitada = direccionOpt.get();

            logger.info("El paciente {} (ID: {}, DNI: {}) ha procesado exitosamente la solicitud de ambulancia con direccion: {} (ID: {}).",
                        MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI), direccionSolicitada.getDireccionCompleta(), direccionId);
            return ResponseEntity.ok(Map.of("message", "Solicitud de ambulancia procesada exitosamente.")); // Retorna un mensaje de exito con estado HTTP 200 OK.

        } catch (IllegalArgumentException e) {
            // Maneja cualquier error interno al procesar la solicitud de ambulancia.
            logger.error("Error interno al procesar la solicitud de ambulancia para el paciente {} (ID: {}, DNI: {}): {}",
                        MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error interno al procesar la solicitud de ambulancia."));
        } finally {
            clearPacienteMDCContext(); // Limpiar MDC al finalizar el metodo del controlador
        }
    }
}