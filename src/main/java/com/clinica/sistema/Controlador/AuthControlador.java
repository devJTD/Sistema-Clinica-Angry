package com.clinica.sistema.Controlador;

import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Servicio.AuthServicio;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.StringUtils; 
import com.google.common.base.Preconditions; 

@Controller
public class AuthControlador {

    @Autowired
    private AuthServicio authServicio;

    private Logger logger = LoggerFactory.getLogger(AuthControlador.class);

    @GetMapping("/login")
    public String mostrarFormularioLogin() {
        logger.info("El usuario ha accedido a la página de login.");
        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam("correo") String correo,
                                @RequestParam("contraseña") String contraseña,
                                HttpSession session) {

        if (StringUtils.isBlank(correo) || StringUtils.isBlank(contraseña)) {
            logger.warn("Intento de login con correo o contraseña vacíos.");
            return "redirect:/login?error=camposvacios";
        }

        Paciente paciente = authServicio.buscarPorCorreoYPassword(correo, contraseña);
        if (paciente != null) {
            session.setAttribute("usuario", paciente);
            logger.info("Login exitoso para el correo: {}", correo);
            return "redirect:/";
        }
        logger.warn("Login fallido para el correo: {}", correo);
        return "redirect:/login?error=credenciales";

    }

    @PostMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        session.invalidate();
        logger.info("Sesión cerrada.");
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

        Preconditions.checkNotNull(paciente, "El objeto Paciente no puede ser nulo.");

        if (StringUtils.isBlank(paciente.getNombre()) ||
            StringUtils.isBlank(paciente.getApellido()) ||
            StringUtils.isBlank(paciente.getCorreo()) ||
            StringUtils.isBlank(paciente.getContraseña()) ||
            StringUtils.isBlank(paciente.getDni())) {
            logger.warn("Registro fallido: campos obligatorios vacíos para el paciente.");
            return "redirect:/registro?error=camposvacios";
        }

        if (authServicio.existePacientePorEmailODni(paciente.getCorreo(), paciente.getDni())) {
            logger.warn("Registro fallido: paciente con correo {} o DNI {} ya existe.", paciente.getCorreo(),
                    paciente.getDni());
            return "redirect:/registro?error=duplicado";
        }

        authServicio.guardarPaciente(paciente);
        logger.info("Paciente registrado exitosamente: {}", paciente.getCorreo());
        return "redirect:/login";
    }
}