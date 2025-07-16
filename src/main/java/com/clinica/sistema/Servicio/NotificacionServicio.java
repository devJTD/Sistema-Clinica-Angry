package com.clinica.sistema.Servicio;

import org.slf4j.Logger; // Importar Logger
import org.slf4j.LoggerFactory; // Importar LoggerFactory
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificacionServicio {

    private final Logger logger = LoggerFactory.getLogger(NotificacionServicio.class); // Instancia del logger

    private final JavaMailSender mailSender;

    @Value("${empresa.email}")
    private String empresaEmail;

    public NotificacionServicio(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarCorreoSimple(String destinatarioEmail, String asunto, String contenidoMensaje) {
        // Validacion basica de los parametros
        if (destinatarioEmail == null || destinatarioEmail.isBlank()) {
            logger.warn("Validacion fallida: No se puede enviar correo, el email del destinatario esta vacio o es nulo.");
            throw new IllegalArgumentException("El correo del destinatario no puede estar vacio.");
        }
        if (asunto == null || asunto.isBlank()) {
            logger.warn("Validacion fallida: No se puede enviar correo a {}, el asunto esta vacio o es nulo.", destinatarioEmail);
            throw new IllegalArgumentException("El asunto del correo no puede estar vacio.");
        }
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
            // Log especifico para excepciones relacionadas con el envio de correo
            logger.error("Error al enviar correo a: {} con asunto: '{}'. Detalle: {}", destinatarioEmail, asunto, e.getMessage(), e);
            // Puedes considerar relanzar la excepcion como una excepcion de negocio si es necesario
            // throw new ServicioCorreoException("No se pudo enviar el correo de notificacion a " + destinatarioEmail, e);
        } catch (Exception e) {
            // Captura y log de cualquier otra excepcion inesperada
            logger.error("Ocurrio un error inesperado al intentar enviar correo a: {} con asunto: '{}'. Detalle: {}", destinatarioEmail, asunto, e.getMessage(), e);
        }
    }
}