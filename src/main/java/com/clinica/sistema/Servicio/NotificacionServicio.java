package com.clinica.sistema.Servicio;

import org.slf4j.Logger; 
import org.slf4j.LoggerFactory; 
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificacionServicio {

    private final Logger logger = LoggerFactory.getLogger(NotificacionServicio.class); 

    private final JavaMailSender mailSender;

    @Value("${empresa.email}")
    private String empresaEmail;

    public NotificacionServicio(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarCorreoSimple(String destinatarioEmail, String asunto, String contenidoMensaje) {
        // Valida que el email del destinatario no esté vacío
        if (destinatarioEmail == null || destinatarioEmail.isBlank()) {
            logger.warn("Validacion fallida: No se puede enviar correo, el email del destinatario esta vacio o es nulo.");
            throw new IllegalArgumentException("El correo del destinatario no puede estar vacio.");
        }
        // Valida que el asunto no esté vacío
        if (asunto == null || asunto.isBlank()) {
            logger.warn("Validacion fallida: No se puede enviar correo a {}, el asunto esta vacio o es nulo.", destinatarioEmail);
            throw new IllegalArgumentException("El asunto del correo no puede estar vacio.");
        }
        // Valida que el contenido del mensaje no esté vacío
        if (contenidoMensaje == null || contenidoMensaje.isBlank()) {
            logger.warn("Validacion fallida: No se puede enviar correo a {}, el contenido del mensaje esta vacio o es nulo. Asunto: {}", destinatarioEmail, asunto);
            throw new IllegalArgumentException("El contenido del mensaje no puede estar vacio.");
        }

        logger.info("Intentando enviar correo simple a: {} con asunto: '{}' desde: {}.", destinatarioEmail, asunto, empresaEmail);

        try {
            SimpleMailMessage mensajeCorreo = new SimpleMailMessage();
            mensajeCorreo.setFrom(empresaEmail);
            mensajeCorreo.setTo(destinatarioEmail);
            mensajeCorreo.setSubject(asunto);
            mensajeCorreo.setText(contenidoMensaje);

            mailSender.send(mensajeCorreo);
            logger.info("Correo enviado exitosamente a: {} con asunto: '{}'.", destinatarioEmail, asunto);

        } catch (MailException e) {
            // Maneja excepciones específicas de envío de correo
            logger.error("Error al enviar correo a: {} con asunto: '{}'. Detalle: {}", destinatarioEmail, asunto, e.getMessage(), e);
            throw new RuntimeException("Fallo al enviar el correo debido a un problema con el servicio de correo.", e);
        } catch (Exception e) {
            // Maneja cualquier otra excepción inesperada
            logger.error("Ocurrio un error inesperado al intentar enviar correo a: {} con asunto: '{}'. Detalle: {}", destinatarioEmail, asunto, e.getMessage(), e);
            throw new RuntimeException("Ocurrió un error inesperado al intentar enviar el correo.", e);
        }
    }
}