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

@Controller
public class AuthControlador {

    // Inyección del servicio que maneja lógica de autenticación y registro
    @Autowired
    private AuthServicio authServicio;

    // Logger para registrar eventos importantes
    private Logger logger = LoggerFactory.getLogger(AuthControlador.class);

    // Muestra la página de login
    @GetMapping("/login")
    public String mostrarFormularioLogin() {
        logger.info("El usuario ha accedido a la página de login.");
        return "login"; // Devuelve la vista login.html
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam("correo") String correo,
            @RequestParam("contraseña") String contraseña,
            HttpSession session) {

        // Usa el método del servicio que ya busca en pacientes.json
        Paciente paciente = authServicio.buscarPorCorreoYPassword(correo, contraseña);
        if (paciente != null) {
            session.setAttribute("usuario", paciente);
            return "redirect:/";
        }
        return "redirect:/login?error";

    }

    // Muestra la página de login tras cerrar sesión
    @PostMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        // Invalida la sesión actual
        session.invalidate();

        // Redirige al login con parámetro de éxito
        return "redirect:/login?logout";
    }

    // Muestra la página de registro
    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("paciente", new Paciente()); // Esto evita el error en Thymeleaf
        return "registro"; // Este es tu archivo registro.html
    }

    /**
     * Procesa los datos del formulario de registro.
     * Verifica si el paciente ya existe por correo o DNI.
     * Si no existe, lo guarda en pacientes.json y redirige al login.
     */
    @PostMapping("/registro")
    public String procesarRegistro(@ModelAttribute Paciente paciente) {
        logger.info("Intentando registrar paciente con correo: {} y DNI: {}", paciente.getCorreo(), paciente.getDni());

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
