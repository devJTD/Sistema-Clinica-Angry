package com.clinica.sistema.Controlador;

import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Servicio.AuthServicio;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class AuthControlador {

    private final AuthServicio authServicio;

    private final Logger logger = LoggerFactory.getLogger(AuthControlador.class);

    public AuthControlador(AuthServicio authServicio) {
        this.authServicio = authServicio;
    }

    @GetMapping("/login")
    public String mostrarFormularioLogin() {
        logger.info("El usuario ha accedido a la página de login.");
        return "login";
    }

    @PostMapping("/logout")
    public String cerrarSesion() {
        logger.info("Sesión cerrada (manejado por Spring Security).");
        return "redirect:/login?logout";
    }

    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("paciente", new Paciente());
        return "registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(@ModelAttribute Paciente paciente) {
        logger.info("Intentando registrar paciente con correo: {} y DNI: {}", paciente.getCorreo(), paciente.getDni());

        try {
            if (paciente.getCorreo() == null || paciente.getCorreo().isBlank() ||
                paciente.getDni() == null || paciente.getDni().isBlank() ||
                paciente.getContraseña() == null || paciente.getContraseña().isBlank()) {
                logger.warn("Registro fallido: campos obligatorios vacíos.");
                return "redirect:/registro?error=camposvacios";
            }

            if (authServicio.existePacientePorEmailODni(paciente.getCorreo(), paciente.getDni())) {
                logger.warn("Registro fallido: paciente con correo {} o DNI {} ya existe.", paciente.getCorreo(), paciente.getDni());
                return "redirect:/registro?error=duplicado";
            }

            authServicio.guardarPaciente(paciente);
            logger.info("Paciente registrado exitosamente: {}", paciente.getCorreo());
            return "redirect:/login?registroExitoso";
        } catch (IllegalArgumentException e) {
            logger.error("Error de validación durante el registro: {}", e.getMessage());
            String errorMessage = e.getMessage().contains("vacío") ? "camposvacios" : "datosinvalidos";
            if(e.getMessage().contains("nulo")) errorMessage = "camposvacios";
            return "redirect:/registro?error=" + errorMessage;
        } catch (Exception e) {
            logger.error("Error inesperado durante el registro para el correo {}: {}", paciente.getCorreo(), e.getMessage(), e);
            return "redirect:/registro?error=errorinesperado";
        }
    }
}