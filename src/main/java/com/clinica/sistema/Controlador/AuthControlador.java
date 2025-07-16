package com.clinica.sistema.Controlador;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.clinica.sistema.Modelo.Direccion;
import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Servicio.AuthServicio;

@Controller // Indica que esta clase es un controlador Spring MVC.
public class AuthControlador {

    private final AuthServicio authServicio; // Servicio para la lógica de autenticación y registro.

    private final Logger logger = LoggerFactory.getLogger(AuthControlador.class); // Logger para registrar eventos.

    // Constructor que inyecta el servicio de autenticación.
    public AuthControlador(AuthServicio authServicio) {
        this.authServicio = authServicio;
    }

    // Maneja las solicitudes GET a /login, mostrando la página de inicio de sesión.
    @GetMapping("/login")
    public String mostrarFormularioLogin() {
        logger.info("El usuario ha accedido a la pagina de login."); // Registra el acceso a la página de login.
        return "login"; // Retorna el nombre de la vista (login.html).
    }

    // Maneja las solicitudes POST a /logout, procesando el cierre de sesión del usuario.
    @PostMapping("/logout")
    public String cerrarSesion() {
        logger.info("El usuario ha cerrado sesion."); // Registra el cierre de sesión.
        return "redirect:/login?logout"; // Redirige a la página de login con un parámetro de logout.
    }

    // Maneja las solicitudes GET a /registro, mostrando el formulario de registro de nuevos pacientes.
    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        logger.info("El usuario ha accedido a la pagina de registro."); // Registra el acceso a la página de registro.
        model.addAttribute("paciente", new Paciente()); // Añade un nuevo objeto Paciente al modelo para el formulario.
        return "registro"; // Retorna el nombre de la vista (registro.html).
    }

    // Maneja las solicitudes POST a /registro, procesando el envío del formulario de registro.
    // Valida los datos del paciente y su dirección, y guarda al nuevo paciente.
    @PostMapping("/registro")
    public String procesarRegistro(
            @ModelAttribute Paciente paciente, // Bindea los datos del formulario a un objeto Paciente.
            @RequestParam("direccion") String direccionCompleta // Captura el parámetro 'direccion' del formulario.
    ) {
        logger.info("Intento de registro de nuevo usuario. Datos recibidos: [Correo: {}, DNI: {}, Nombre: {}, Apellido: {}, Direccion: {}]",
                paciente.getCorreo(), paciente.getDni(), paciente.getNombre(), paciente.getApellido(), direccionCompleta);

        try {
            // **Validación de campos obligatorios:**
            // Verifica que ninguno de los campos esenciales del paciente esté vacío o nulo.
            if (paciente.getCorreo() == null || paciente.getCorreo().isBlank() ||
                paciente.getDni() == null || paciente.getDni().isBlank() ||
                paciente.getContraseña() == null || paciente.getContraseña().isBlank() ||
                paciente.getNombre() == null || paciente.getNombre().isBlank() ||
                paciente.getApellido() == null || paciente.getApellido().isBlank()) {
                logger.warn("Registro fallido: Campos obligatorios vacios para el correo: {} o DNI: {}.", paciente.getCorreo(), paciente.getDni());
                return "redirect:/registro?error=camposvacios"; // Redirige con un error si faltan campos.
            }

            // **Validación de la dirección:**
            // Verifica que la dirección completa no esté vacía o nula.
            if (direccionCompleta == null || direccionCompleta.isBlank()) {
                logger.warn("Registro fallido: Direccion vacia para el correo: {} o DNI: {}.", paciente.getCorreo(), paciente.getDni());
                return "redirect:/registro?error=direccionvacia"; // Redirige con un error si la dirección está vacía.
            }

            // **Verificación de duplicados:**
            // Comprueba si ya existe un paciente con el mismo correo o DNI.
            if (authServicio.existePacientePorEmailODni(paciente.getCorreo(), paciente.getDni())) {
                logger.warn("Registro fallido: Ya existe un usuario con el correo: {} o DNI: {}.", paciente.getCorreo(), paciente.getDni());
                return "redirect:/registro?error=duplicado"; // Redirige con un error si el usuario ya existe.
            }

            // Crea un objeto Direccion y asigna la dirección completa.
            Direccion direccion = new Direccion();
            direccion.setDireccionCompleta(direccionCompleta);

            // **Guarda el paciente y su dirección asociada:**
            // Llama al servicio para guardar el nuevo paciente junto con su dirección.
            authServicio.guardarPaciente(paciente, direccion);

            logger.info("Registro exitoso para el usuario con DNI: {}.", paciente.getDni()); // Registra el éxito del registro.
            return "redirect:/login?registroExitoso"; // Redirige a la página de login con un mensaje de éxito.

        } catch (IllegalArgumentException e) {
            // Maneja excepciones de argumentos inválidos (ej. validaciones de negocio del servicio).
            logger.error("Error de validacion durante el registro del usuario con correo {}: {}", paciente.getCorreo(), e.getMessage());
            String errorMessage = "errorinesperado";
            if (e.getMessage().contains("vacio") || e.getMessage().contains("nulo")) {
                errorMessage = "camposvacios";
            } else if (e.getMessage().contains("existe")) {
                errorMessage = "duplicado";
            }
            return "redirect:/registro?error=" + errorMessage; // Redirige con un error específico.
        } catch (Exception e) {
            // Maneja cualquier otra excepción inesperada durante el proceso de registro.
            logger.error("Error inesperado al intentar registrar al usuario con correo {}: {}", paciente.getCorreo(), e.getMessage(), e);
            return "redirect:/registro?error=errorinesperado"; // Redirige con un error genérico.
        }
    }
}