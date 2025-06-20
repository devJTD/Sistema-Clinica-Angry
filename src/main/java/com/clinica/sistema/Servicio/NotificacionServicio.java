package com.clinica.sistema.Servicio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NotificacionServicio {

    private final JavaMailSender mailSender;

    @Value("${empresa.email}")
    private String empresaEmail;

    private final Logger logger = LoggerFactory.getLogger(NotificacionServicio.class);

    public NotificacionServicio(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        logger.info("[NotificacionServicio] - Servicio inicializado. JavaMailSender inyectado.");
    }

    public void enviarCorreoSimple(String destinatarioEmail, String asunto, String contenidoMensaje) {
        logger.info("[NotificacionServicio] - INICIO: Solicitud de envío de correo simple.");
        logger.info("[NotificacionServicio] - Destinatario: '{}', Asunto: '{}'", destinatarioEmail, asunto);
        logger.debug("[NotificacionServicio] - Contenido del mensaje:\n{}", contenidoMensaje); // Útil para depurar el formato del mensaje

        try {
            SimpleMailMessage mensajeCorreo = new SimpleMailMessage();
            mensajeCorreo.setFrom(empresaEmail);
            mensajeCorreo.setTo(destinatarioEmail);
            mensajeCorreo.setSubject(asunto);
            mensajeCorreo.setText(contenidoMensaje);
            logger.debug("[NotificacionServicio] - Objeto SimpleMailMessage creado. Remitente: '{}', Destinatario: '{}', Asunto: '{}'", empresaEmail, destinatarioEmail, asunto);

            logger.info("[NotificacionServicio] - Intentando enviar correo a través de JavaMailSender...");
            mailSender.send(mensajeCorreo);
            logger.info("[NotificacionServicio] - Correo enviado exitosamente a: '{}'", destinatarioEmail);

        } catch (MailException e) {
            logger.error("[NotificacionServicio] - ERROR: Fallo al enviar el correo a '{}'. Mensaje de error: {}. Detalles completos de la excepción:", destinatarioEmail, e.getMessage(), e);
            // Aquí puedes lanzar una excepción personalizada si el envío de correo es crítico
            // throw new ServicioCorreoException("No se pudo enviar el correo de notificación.", e);
        } catch (Exception e) { // Captura cualquier otra excepción inesperada
            logger.error("[NotificacionServicio] - ERROR INESPERADO: Se produjo una excepción no relacionada con MailException al enviar correo a '{}'. Mensaje: {}. Detalles:", destinatarioEmail, e.getMessage(), e);
        }
        logger.info("[NotificacionServicio] - FIN: Proceso de envío de correo simple.");
    }
}