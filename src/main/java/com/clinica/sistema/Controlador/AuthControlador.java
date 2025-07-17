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

    // Constructor que inyecta el servicio de autenticacion.
    public AuthControlador(AuthServicio authServicio) {
        this.authServicio = authServicio;
    }

    // Maneja las solicitudes GET a /login, mostrando la pagina de inicio de sesion.
    @GetMapping("/login")
    public String mostrarFormularioLogin() {
        logger.info("El usuario ha accedido a la pagina de login.");
        return "login"; 
    }

    // Metodo para procesar el login con validacion manual
    @PostMapping("/login")
    public String procesarLogin(
            @RequestParam("correo") String correo,
            @RequestParam("contraseña") String contraseña,
            Model model
    ) {
        // Log de los datos recibidos (antes de cualquier validacion)
        logger.info("Intento de login. Datos recibidos: [Correo: '{}', Contrasena_longitud_ingresada: '{}']",
                    correo, (contraseña != null ? contraseña.length() : "N/A - Contrasena nula"));

        // Validacion manual de Correo
        if (correo == null || correo.isBlank()) {
            logger.warn("Fallo de login: Correo electronico vacio. Valor ingresado: '{}'", correo);
            model.addAttribute("error", "El correo electronico no puede estar vacio.");
            return "login";
        }
        
        // Validacion de formato de correo (una expresion regular simple)
        if (!correo.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")) {
            logger.warn("Fallo de login: Formato de correo electronico invalido. Valor ingresado: '{}'", correo);
            model.addAttribute("error", "Por favor, ingresa un correo electronico valido.");
            return "login";
        }

        // Validacion manual de Contrasena
        if (contraseña == null || contraseña.isBlank()) {
            logger.warn("Fallo de login: Contrasena vacia. Correo ingresado: '{}'", correo);
            model.addAttribute("error", "La contrasena no puede estar vacia.");
            return "login";
        }

        logger.warn("Fallo de login: Validaciones de formato y nulidad pasadas, pero autenticacion no implementada aun. Correo: '{}'", correo);
        model.addAttribute("error", "Usuario o contrasena incorrectos.");
        return "login";
    }

    // Maneja las solicitudes POST a /logout, procesando el cierre de sesion del usuario.
    @PostMapping("/logout")
    public String cerrarSesion() {
        logger.info("El usuario ha cerrado sesion."); 
        return "redirect:/login?logout"; 
    }

    // Maneja las solicitudes GET a /registro, mostrando el formulario de registro de nuevos pacientes.
    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        logger.info("El usuario ha accedido a la pagina de registro."); 
        model.addAttribute("paciente", new Paciente());
        return "registro"; 
    }

    // Maneja las solicitudes POST a /registro, procesando el envio del formulario de registro.
    // Valida los datos del paciente y su direccion, y guarda al nuevo paciente.
    @PostMapping("/registro")
    public String procesarRegistro(
            @ModelAttribute @Valid Paciente paciente, 
            BindingResult bindingResult,
            @RequestParam("direccionCompleta") String direccionCompleta,
            @RequestParam("contraseña") String rawContraseña,
            Model model
    ) {
        // Detalles de los datos recibidos (antes de cualquier validacion)
        logger.info("Intento de registro de nuevo usuario. Datos recibidos: [Nombre: '{}', Apellido: '{}', DNI: '{}', Telefono: '{}', Correo: '{}', Contrasena_longitud_ingresada: '{}', Direccion: '{}']",
                paciente.getNombre(), paciente.getApellido(), paciente.getDni(), paciente.getTelefono(), paciente.getCorreo(),
                (rawContraseña != null ? rawContraseña.length() : "N/A - Contrasena nula"), direccionCompleta);

        // VALIDACION DE CONTRASENA
        if (rawContraseña == null || rawContraseña.isBlank()) {
            bindingResult.rejectValue("contraseña", "contrasena.vacia", "La contrasena no puede estar vacia.");
            logger.error("Error de validacion de contrasena: Contrasena vacia. Valor ingresado: '{}'", rawContraseña);
        } else if (rawContraseña.length() < 8 || rawContraseña.length() > 30) {
            bindingResult.rejectValue("contraseña", "contrasena.longitud", "La contrasena debe tener entre 8 y 30 caracteres.");
            logger.error("Error de validacion de contrasena: Longitud invalida. Valor ingresado: '{}', Longitud: {}", rawContraseña, rawContraseña.length());
        } else {
            // Si la contrasena cumple la validacion, la establecemos en el objeto paciente.
            paciente.setContraseña(rawContraseña); 
        }

        // VALIDACION MANUAL DE CORREO ELECTRONICO (para .com o similar)
        if (paciente.getCorreo() != null && !paciente.getCorreo().matches(".*\\.(com|org|net|es|io|co|info|biz|gob|edu)$")) {
            bindingResult.rejectValue("correo", "correo.dominioInvalido", "El correo electronico debe terminar con un dominio valido como .com, .org, .net, etc.");
            logger.error("Error de validacion de correo: Dominio invalido. Valor ingresado: '{}'", paciente.getCorreo());
        }

        // VALIDACION DE DIRECCION (usando las anotaciones de Direccion.java)
        Direccion tempDireccion = new Direccion();
        tempDireccion.setDireccionCompleta(direccionCompleta);
        
        jakarta.validation.Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        java.util.Set<ConstraintViolation<Direccion>> violations = validator.validate(tempDireccion);

        if (!violations.isEmpty()) {
            for (ConstraintViolation<Direccion> violation : violations) {
                bindingResult.rejectValue("direcciones", "direccionCompleta." + violation.getPropertyPath().toString(), violation.getMessage());
                logger.error("Error de validacion en la direccion. Campo: '{}', Valor ingresado: '{}', Mensaje: '{}'", 
                             violation.getPropertyPath().toString(), direccionCompleta, violation.getMessage());
            }
        }

        // MANEJO DE ERRORES DE VALIDACION (incluyendo la validacion manual de contrasena y correo)
        if (bindingResult.hasErrors()) {
            logger.error("Registro fallido: Se encontraron errores de validacion en el formulario.");
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
            // VERIFICACION DE DUPLICADOS dni y correo
            if (authServicio.existePacientePorEmailODni(paciente.getCorreo(), paciente.getDni())) {
                String errorMessage = "Ya existe un paciente con el correo: " + paciente.getCorreo() + " o DNI: " + paciente.getDni() + ".";
                logger.error("Error de logica de negocio: {}", errorMessage);
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
            // Errores de logica de negocio o validaciones lanzadas desde el servicio (si las hubiera)
            logger.error("Excepcion de argumento invalido o logica de negocio durante el registro del usuario con correo {}: {}. Detalles: {}", paciente.getCorreo(), e.getMessage(), e.toString());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("paciente", paciente);
            model.addAttribute("direccionCompleta", direccionCompleta);
            return "registro";
        } catch (Exception e) {
            // Cualquier otra excepcion inesperada
            logger.error("Error inesperado al intentar registrar al usuario con correo {}: {}. Stack trace completo:", paciente.getCorreo(), e.getMessage(), e);
            model.addAttribute("error", "Ocurrio un error inesperado al procesar su registro. Intentelo de nuevo mas tarde.");
            model.addAttribute("paciente", paciente);
            model.addAttribute("direccionCompleta", direccionCompleta);
            return "registro";
        }
    }
}