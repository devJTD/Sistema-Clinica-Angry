package com.clinica.sistema.Controlador;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory; // Importar la nueva entidad Direccion
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
    public String procesarRegistro(
            @ModelAttribute Paciente paciente,
            @RequestParam("direccion") String direccionCompleta // Captura el campo 'direccion' del formulario
    ) {
        logger.info("Intentando registrar paciente con correo: {} y DNI: {}", paciente.getCorreo(), paciente.getDni());
        logger.info("Dirección proporcionada: {}", direccionCompleta);

        try {
            // Validaciones básicas de campos del paciente
            if (paciente.getCorreo() == null || paciente.getCorreo().isBlank() ||
                paciente.getDni() == null || paciente.getDni().isBlank() ||
                paciente.getContraseña() == null || paciente.getContraseña().isBlank() ||
                paciente.getNombre() == null || paciente.getNombre().isBlank() || // Aseguramos que nombre y apellido no estén vacíos
                paciente.getApellido() == null || paciente.getApellido().isBlank()) {
                logger.warn("Registro fallido: campos obligatorios de paciente vacíos.");
                return "redirect:/registro?error=camposvacios";
            }

            // Validación de la dirección
            if (direccionCompleta == null || direccionCompleta.isBlank()) {
                logger.warn("Registro fallido: el campo de dirección está vacío.");
                return "redirect:/registro?error=direccionvacia";
            }

            if (authServicio.existePacientePorEmailODni(paciente.getCorreo(), paciente.getDni())) {
                logger.warn("Registro fallido: paciente con correo {} o DNI {} ya existe.", paciente.getCorreo(), paciente.getDni());
                return "redirect:/registro?error=duplicado";
            }

            // Crear la entidad Direccion
            Direccion direccion = new Direccion();
            direccion.setDireccionCompleta(direccionCompleta);
            // La relación con paciente se establecerá en el servicio o se asume que se propaga vía CascadeType.ALL

            // Llamar al servicio para guardar el paciente y la dirección
            authServicio.guardarPaciente(paciente, direccion); 

            logger.info("Paciente y dirección registrados exitosamente para el correo: {}", paciente.getCorreo());
            return "redirect:/login?registroExitoso";

        } catch (IllegalArgumentException e) {
            logger.error("Error de validación durante el registro: {}", e.getMessage());
            String errorMessage = "errorinesperado"; // Default error message
            if (e.getMessage().contains("vacío") || e.getMessage().contains("nulo")) {
                errorMessage = "camposvacios";
            } else if (e.getMessage().contains("existe")) {
                errorMessage = "duplicado";
            }
            return "redirect:/registro?error=" + errorMessage;
        } catch (Exception e) {
            logger.error("Error inesperado durante el registro para el correo {}: {}", paciente.getCorreo(), e.getMessage(), e);
            return "redirect:/registro?error=errorinesperado";
        }
    }
}