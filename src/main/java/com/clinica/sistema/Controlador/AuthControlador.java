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

@Controller
public class AuthControlador {

    private final AuthServicio authServicio;

    private final Logger logger = LoggerFactory.getLogger(AuthControlador.class);

    public AuthControlador(AuthServicio authServicio) {
        this.authServicio = authServicio;
    }

    @GetMapping("/login")
    public String mostrarFormularioLogin() {
        logger.info("El usuario ha accedido a la pagina de login.");
        return "login";
    }

    @PostMapping("/logout")
    public String cerrarSesion() {
        logger.info("El usuario ha cerrado sesion.");
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
            @ModelAttribute Paciente paciente,
            @RequestParam("direccion") String direccionCompleta
    ) {
        logger.info("Intento de registro de nuevo usuario. Datos recibidos: [Correo: {}, DNI: {}, Nombre: {}, Apellido: {}, Direccion: {}]",
                paciente.getCorreo(), paciente.getDni(), paciente.getNombre(), paciente.getApellido(), direccionCompleta);

        try {
            if (paciente.getCorreo() == null || paciente.getCorreo().isBlank() ||
                paciente.getDni() == null || paciente.getDni().isBlank() ||
                paciente.getContraseña() == null || paciente.getContraseña().isBlank() || // "Contraseña" contains 'ñ', which is a character, not an accent. It stays.
                paciente.getNombre() == null || paciente.getNombre().isBlank() || 
                paciente.getApellido() == null || paciente.getApellido().isBlank()) {
                logger.warn("Registro fallido: Campos obligatorios vacios para el correo: {} o DNI: {}.", paciente.getCorreo(), paciente.getDni());
                return "redirect:/registro?error=camposvacios";
            }

            if (direccionCompleta == null || direccionCompleta.isBlank()) {
                logger.warn("Registro fallido: Direccion vacia para el correo: {} o DNI: {}.", paciente.getCorreo(), paciente.getDni());
                return "redirect:/registro?error=direccionvacia";
            }

            if (authServicio.existePacientePorEmailODni(paciente.getCorreo(), paciente.getDni())) {
                logger.warn("Registro fallido: Ya existe un usuario con el correo: {} o DNI: {}.", paciente.getCorreo(), paciente.getDni());
                return "redirect:/registro?error=duplicado";
            }

            Direccion direccion = new Direccion();
            direccion.setDireccionCompleta(direccionCompleta);

            authServicio.guardarPaciente(paciente, direccion); 

            logger.info("Registro exitoso para el usuario con DNI: {}.", paciente.getDni());
            return "redirect:/login?registroExitoso";

        } catch (IllegalArgumentException e) {
            logger.error("Error de validacion durante el registro del usuario con correo {}: {}", paciente.getCorreo(), e.getMessage());
            String errorMessage = "errorinesperado";
            if (e.getMessage().contains("vacio") || e.getMessage().contains("nulo")) {
                errorMessage = "camposvacios";
            } else if (e.getMessage().contains("existe")) {
                errorMessage = "duplicado";
            }
            return "redirect:/registro?error=" + errorMessage;
        } catch (Exception e) {
            logger.error("Error inesperado al intentar registrar al usuario con correo {}: {}", paciente.getCorreo(), e.getMessage(), e);
            return "redirect:/registro?error=errorinesperado";
        }
    }
}