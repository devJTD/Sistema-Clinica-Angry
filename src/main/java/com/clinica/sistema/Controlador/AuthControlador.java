package com.clinica.sistema.Controlador;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError; 
import jakarta.validation.Valid; 
import jakarta.validation.ConstraintViolation; 
import jakarta.validation.Validation;

import com.clinica.sistema.Modelo.Direccion;
import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Servicio.AuthServicio;

@Controller
public class AuthControlador {

    private final AuthServicio authServicio;

    private final Logger logger = LoggerFactory.getLogger(AuthControlador.class); 

    // Constructor que inyecta el servicio de autenticación.
    public AuthControlador(AuthServicio authServicio) {
        this.authServicio = authServicio;
    }

    // Maneja las solicitudes GET a /login, mostrando la página de inicio de sesión.
    @GetMapping("/login")
    public String mostrarFormularioLogin() {
        logger.info("El usuario ha accedido a la página de login.");
        return "login"; 
    }

    // Maneja las solicitudes POST a /logout, procesando el cierre de sesión del usuario.
    @PostMapping("/logout")
    public String cerrarSesion() {
        logger.info("El usuario ha cerrado sesión.");
        return "redirect:/login?logout"; 
    }

    // Maneja las solicitudes GET a /registro, mostrando el formulario de registro de nuevos pacientes.
    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        logger.info("El usuario ha accedido a la página de registro."); 
        model.addAttribute("paciente", new Paciente());
        return "registro"; 
    }

    // Maneja las solicitudes POST a /registro, procesando el envío del formulario de registro.
    // Valida los datos del paciente y su dirección, y guarda al nuevo paciente.
    @PostMapping("/registro")
    public String procesarRegistro(
            @ModelAttribute @Valid Paciente paciente, 
            BindingResult bindingResult,
            @RequestParam("direccionCompleta") String direccionCompleta,
            @RequestParam("contraseña") String rawContraseña,
            Model model
    ) {
        // Detalles de los datos recibidos (antes de cualquier validación) ---
        logger.info("Intento de registro de nuevo usuario. Datos recibidos: [Nombre: '{}', Apellido: '{}', DNI: '{}', Teléfono: '{}', Correo: '{}', Contraseña_longitud_ingresada: '{}', Direccion: '{}']",
                paciente.getNombre(), paciente.getApellido(), paciente.getDni(), paciente.getTelefono(), paciente.getCorreo(),
                (rawContraseña != null ? rawContraseña.length() : "N/A - Contraseña nula"), direccionCompleta);

        // VALIDACIÓN DE CONTRASEÑA
        if (rawContraseña == null || rawContraseña.isBlank()) {
            bindingResult.rejectValue("contraseña", "contrasena.vacia", "La contraseña no puede estar vacía.");
            logger.error("Error de validación de contraseña: Contraseña vacía. Valor ingresado: '{}'", rawContraseña);
        } else if (rawContraseña.length() < 8 || rawContraseña.length() > 30) {
            bindingResult.rejectValue("contraseña", "contrasena.longitud", "La contraseña debe tener entre 8 y 30 caracteres.");
            logger.error("Error de validación de contraseña: Longitud inválida. Valor ingresado: '{}', Longitud: {}", rawContraseña, rawContraseña.length());
        } else {
            // Si la contraseña cumple la validación, la establecemos en el objeto paciente.
            paciente.setContraseña(rawContraseña); 
        }

        // --- VALIDACIÓN MANUAL DE CORREO ELECTRÓNICO (para .com o similar) ---
        // Aunque @Email y @Pattern ya validan el formato, esta es una validación extra solicitada para .com
        if (paciente.getCorreo() != null && !paciente.getCorreo().matches(".*\\.(com|org|net|es|io|co|info|biz|gob|edu)$")) {
            bindingResult.rejectValue("correo", "correo.dominioInvalido", "El correo electrónico debe terminar con un dominio válido como .com, .org, .net, etc.");
            logger.error("Error de validación de correo: Dominio inválido. Valor ingresado: '{}'", paciente.getCorreo());
        }

        // --- VALIDACIÓN DE DIRECCIÓN (usando las anotaciones de Direccion.java) ---
        Direccion tempDireccion = new Direccion();
        tempDireccion.setDireccionCompleta(direccionCompleta);
        
        jakarta.validation.Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        java.util.Set<ConstraintViolation<Direccion>> violations = validator.validate(tempDireccion);

        if (!violations.isEmpty()) {
            for (ConstraintViolation<Direccion> violation : violations) {
                bindingResult.rejectValue("direcciones", "direccionCompleta." + violation.getPropertyPath().toString(), violation.getMessage());
                logger.error("Error de validación en la dirección. Campo: '{}', Valor ingresado: '{}', Mensaje: '{}'", 
                             violation.getPropertyPath().toString(), direccionCompleta, violation.getMessage());
            }
        }

        // MANEJO DE ERRORES DE VALIDACIÓN (incluyendo la validación manual de contraseña y correo)
        if (bindingResult.hasErrors()) {
            logger.error("Registro fallido: Se encontraron errores de validación en el formulario.");
            for (FieldError error : bindingResult.getFieldErrors()) {
                logger.error("Campo: '{}', Valor ingresado: '{}', Mensaje de error: '{}'",
                             error.getField(), error.getRejectedValue(), error.getDefaultMessage());
            }
            model.addAttribute("error", "Por favor, corrija los errores en el formulario.");
            model.addAttribute("paciente", paciente);
            model.addAttribute("direccionCompleta", direccionCompleta);
            return "registro";
        }

        try {
            // VERIFICACIÓN DE DUPLICADOS dni y correo
            if (authServicio.existePacientePorEmailODni(paciente.getCorreo(), paciente.getDni())) {
                String errorMessage = "Ya existe un paciente con el correo: " + paciente.getCorreo() + " o DNI: " + paciente.getDni() + ".";
                logger.error("Error de lógica de negocio: {}", errorMessage);
                model.addAttribute("error", errorMessage); 
                model.addAttribute("paciente", paciente);
                model.addAttribute("direccionCompleta", direccionCompleta);
                return "registro";
            }

            // Creamos el objeto Direccion para asociarlo al paciente
            Direccion direccion = new Direccion();
            direccion.setDireccionCompleta(direccionCompleta);

            // GUARDAR PACIENTE
            authServicio.guardarPaciente(paciente, direccion);

            logger.info("Registro exitoso para el usuario con DNI: {}.", paciente.getDni());
            return "redirect:/login?registroExitoso";

        } catch (IllegalArgumentException e) {
            // Errores de lógica de negocio o validaciones lanzadas desde el servicio (si las hubiera)
            logger.error("Excepción de argumento inválido o lógica de negocio durante el registro del usuario con correo {}: {}. Detalles: {}", paciente.getCorreo(), e.getMessage(), e.toString());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("paciente", paciente);
            model.addAttribute("direccionCompleta", direccionCompleta);
            return "registro";
        } catch (Exception e) {
            // Cualquier otra excepción inesperada
            logger.error("Error inesperado al intentar registrar al usuario con correo {}: {}. Stack trace completo:", paciente.getCorreo(), e.getMessage(), e);
            model.addAttribute("error", "Ocurrió un error inesperado al procesar su registro. Inténtelo de nuevo más tarde.");
            model.addAttribute("paciente", paciente);
            model.addAttribute("direccionCompleta", direccionCompleta);
            return "registro";
        }
    }
}