package com.clinica.sistema.Controlador;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.clinica.sistema.Modelo.Direccion;
import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Servicio.AuthServicio;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;

@Controller
public class AuthControlador {

    private final AuthServicio authServicio;

    private final Logger logger = LoggerFactory.getLogger(AuthControlador.class); 

    private static final String MDC_USER_FULL_NAME = "userFullName";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_USER_DNI = "userDni";

    public AuthControlador(AuthServicio authServicio) {
        this.authServicio = authServicio;
    }

    @GetMapping("/login")
    public String mostrarFormularioLogin() {
        logger.info("El usuario ha accedido a la pagina de login.");
        return "login"; 
    }

    @PostMapping("/login")
    public String procesarLogin(
            @RequestParam("correo") String correo,
            @RequestParam("contraseña") String contraseña,
            Model model
    ) {
        logger.info("Intento de login. Datos recibidos: [Correo: '{}', Contrasena_longitud_ingresada: '{}']",
                    correo, (contraseña != null ? contraseña.length() : "N/A - Contrasena nula"));

        if (correo == null || correo.isBlank()) {
            logger.warn("Fallo de login: Correo electronico vacio. Valor ingresado: '{}'", correo);
            model.addAttribute("error", "El correo electronico no puede estar vacio.");
            return "login";
        }
        
        if (!correo.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")) {
            logger.warn("Fallo de login: Formato de correo electronico invalido. Valor ingresado: '{}'", correo);
            model.addAttribute("error", "Por favor, ingresa un correo electronico valido.");
            return "login";
        }

        if (contraseña == null || contraseña.isBlank()) {
            logger.warn("Fallo de login: Contrasena vacia. Correo ingresado: '{}'", correo);
            model.addAttribute("error", "La contrasena no puede estar vacia.");
            return "login";
        }

        // Aquí es donde debería ir tu lógica de autenticación real.
        // Si el login es exitoso y obtienes un objeto Paciente autenticado,
        // establece la información en el MDC en ese punto.

        // Ejemplo de cómo se establecería el MDC *si* la autenticación fuera exitosa y tuvieras un objeto Paciente:
        // Paciente pacienteAutenticado = authServicio.autenticarUsuario(correo, contraseña); 
        // if (pacienteAutenticado != null) {
        //     MDC.put(MDC_USER_FULL_NAME, pacienteAutenticado.getNombre() + " " + pacienteAutenticado.getApellido());
        //     MDC.put(MDC_USER_ID, String.valueOf(pacienteAutenticado.getId()));
        //     MDC.put(MDC_USER_DNI, pacienteAutenticado.getDni());
        //     logger.info("El usuario {} (ID: {}, DNI: {}) ha iniciado sesión exitosamente.", 
        //                  MDC.get(MDC_USER_FULL_NAME), MDC.get(MDC_USER_ID), MDC.get(MDC_USER_DNI));
        //     return "redirect:/dashboard"; 
        // } else {
        //     logger.warn("Fallo de login: Credenciales incorrectas para el correo: '{}'", correo);
        //     model.addAttribute("error", "Usuario o contrasena incorrectos.");
        //     return "login";
        // }

        logger.warn("Fallo de login: Validaciones de formato y nulidad pasadas, pero autenticacion no implementada aun. Correo: '{}'", correo);
        model.addAttribute("error", "Usuario o contrasena incorrectos.");
        return "login";
    }

    @PostMapping("/logout")
    public String cerrarSesion() {
        String userFullName = MDC.get(MDC_USER_FULL_NAME);
        String userId = MDC.get(MDC_USER_ID);
        String userDni = MDC.get(MDC_USER_DNI);

        MDC.remove(MDC_USER_FULL_NAME);
        MDC.remove(MDC_USER_ID);
        MDC.remove(MDC_USER_DNI);

        logger.info("El usuario {} (ID: {}, DNI: {}) ha cerrado sesion.", 
                    userFullName != null ? userFullName : "Desconocido", 
                    userId != null ? userId : "N/A",
                    userDni != null ? userDni : "N/A"); 
        return "redirect:/login?logout"; 
    }

    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        logger.info("El usuario ha accedido a la pagina de registro."); 
        model.addAttribute("paciente", new Paciente());
        return "registro"; 
    }

    @PostMapping("/registro")
    public String procesarRegistro(
            @ModelAttribute @Valid Paciente paciente, 
            BindingResult bindingResult,
            @RequestParam("direccionCompleta") String direccionCompleta,
            @RequestParam("contraseña") String rawContraseña,
            Model model
    ) {
        logger.info("Intento de registro de nuevo usuario. Datos recibidos: [Nombre: '{}', Apellido: '{}', DNI: '{}', Telefono: '{}', Correo: '{}', Contrasena_longitud_ingresada: '{}', Direccion: '{}']",
                paciente.getNombre(), paciente.getApellido(), paciente.getDni(), paciente.getTelefono(), paciente.getCorreo(),
                (rawContraseña != null ? rawContraseña.length() : "N/A - Contrasena nula"), direccionCompleta);

        if (rawContraseña == null || rawContraseña.isBlank()) {
            bindingResult.rejectValue("contraseña", "contrasena.vacia", "La contrasena no puede estar vacia.");
            logger.error("Error de validacion de contrasena: Contrasena vacia. Valor ingresado: '{}'", rawContraseña);
        } else if (rawContraseña.length() < 8 || rawContraseña.length() > 30) {
            bindingResult.rejectValue("contraseña", "contrasena.longitud", "La contrasena debe tener entre 8 y 30 caracteres.");
            logger.error("Error de validacion de contrasena: Longitud invalida. Valor ingresado: '{}', Longitud: {}", rawContraseña, rawContraseña.length());
        } else {
            paciente.setContraseña(rawContraseña); 
        }

        if (paciente.getCorreo() != null && !paciente.getCorreo().matches(".*\\.(com|org|net|es|io|co|info|biz|gob|edu)$")) {
            bindingResult.rejectValue("correo", "correo.dominioInvalido", "El correo electronico debe terminar con un dominio valido como .com, .org, .net, etc.");
            logger.error("Error de validacion de correo: Dominio invalido. Valor ingresado: '{}'", paciente.getCorreo());
        }

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
            if (authServicio.existePacientePorEmailODni(paciente.getCorreo(), paciente.getDni())) {
                String errorMessage = "Ya existe un paciente con el correo: " + paciente.getCorreo() + " o DNI: " + paciente.getDni() + ".";
                logger.error("Error de logica de negocio: {}", errorMessage);
                model.addAttribute("error", errorMessage); 
                model.addAttribute("paciente", paciente);
                model.addAttribute("direccionCompleta", direccionCompleta);
                return "registro";
            }

            Direccion direccion = new Direccion();
            direccion.setDireccionCompleta(direccionCompleta);

            authServicio.guardarPaciente(paciente, direccion);

            logger.info("Registro exitoso para el usuario con DNI: {}.", paciente.getDni());
            return "redirect:/login?registroExitoso";

        } catch (IllegalArgumentException e) {
            logger.error("Excepcion de argumento invalido o logica de negocio durante el registro del usuario con correo {}: {}. Detalles: {}", paciente.getCorreo(), e.getMessage(), e.toString());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("paciente", paciente);
            model.addAttribute("direccionCompleta", direccionCompleta);
            return "registro";
        } catch (Exception e) {
            logger.error("Error inesperado al intentar registrar al usuario con correo {}: {}. Stack trace completo:", paciente.getCorreo(), e.getMessage(), e);
            model.addAttribute("error", "Ocurrio un error inesperado al procesar su registro. Intentelo de nuevo mas tarde.");
            model.addAttribute("paciente", paciente);
            model.addAttribute("direccionCompleta", direccionCompleta);
            return "registro";
        }
    }
}