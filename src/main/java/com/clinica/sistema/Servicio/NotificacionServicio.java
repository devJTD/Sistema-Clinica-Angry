package com.clinica.sistema.Servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC; // Importar la clase MDC
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

    // Constantes para las claves MDC de la notificación
    private static final String MDC_EMAIL_RECIPIENT = "emailRecipient";
    private static final String MDC_EMAIL_SUBJECT = "emailSubject";

    public NotificacionServicio(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Método auxiliar para establecer información de la notificación en el MDC
    private void setNotificacionMDCContext(String destinatarioEmail, String asunto) {
        MDC.put(MDC_EMAIL_RECIPIENT, destinatarioEmail);
        MDC.put(MDC_EMAIL_SUBJECT, asunto);
    }

    // Método auxiliar para limpiar información de la notificación del MDC
    private void clearNotificacionMDCContext() {
        MDC.remove(MDC_EMAIL_RECIPIENT);
        MDC.remove(MDC_EMAIL_SUBJECT);
    }

    public void enviarCorreoSimple(String destinatarioEmail, String asunto, String contenidoMensaje) {
        setNotificacionMDCContext(destinatarioEmail, asunto); // Establecer MDC al inicio del método
        try {
            // Valida que el email del destinatario no esté vacío
            if (destinatarioEmail == null || destinatarioEmail.isBlank()) {
                logger.warn("Validación fallida: No se puede enviar correo, el email del destinatario está vacío o es nulo.");
                throw new IllegalArgumentException("El correo del destinatario no puede estar vacío.");
            }
            // Valida que el asunto no esté vacío
            if (asunto == null || asunto.isBlank()) {
                logger.warn("Validación fallida: No se puede enviar correo a {}, el asunto está vacío o es nulo.", MDC.get(MDC_EMAIL_RECIPIENT));
                throw new IllegalArgumentException("El asunto del correo no puede estar vacío.");
            }
            // Valida que el contenido del mensaje no esté vacío
            if (contenidoMensaje == null || contenidoMensaje.isBlank()) {
                logger.warn("Validación fallida: No se puede enviar correo a {} con asunto '{}', el contenido del mensaje está vacío o es nulo.", MDC.get(MDC_EMAIL_RECIPIENT), MDC.get(MDC_EMAIL_SUBJECT));
                throw new IllegalArgumentException("El contenido del mensaje no puede estar vacío.");
            }

            logger.info("Intentando enviar correo simple a: {} con asunto: '{}' desde: {}.", MDC.get(MDC_EMAIL_RECIPIENT), MDC.get(MDC_EMAIL_SUBJECT), empresaEmail);

            SimpleMailMessage mensajeCorreo = new SimpleMailMessage();
            mensajeCorreo.setFrom(empresaEmail);
            mensajeCorreo.setTo(destinatarioEmail);
            mensajeCorreo.setSubject(asunto);
            mensajeCorreo.setText(contenidoMensaje);

            mailSender.send(mensajeCorreo);
            logger.info("Correo enviado exitosamente a: {} con asunto: '{}'.", MDC.get(MDC_EMAIL_RECIPIENT), MDC.get(MDC_EMAIL_SUBJECT));

        } catch (MailException e) {
            // Maneja excepciones específicas de envío de correo
            logger.error("Error al enviar correo a: {} con asunto: '{}'. Detalle: {}", MDC.get(MDC_EMAIL_RECIPIENT), MDC.get(MDC_EMAIL_SUBJECT), e.getMessage(), e);
            throw new RuntimeException("Fallo al enviar el correo debido a un problema con el servicio de correo.", e);
        } catch (IllegalArgumentException e) {
            // Maneja cualquier otra excepción inesperada
            logger.error("Ocurrió un error inesperado al intentar enviar correo a: {} con asunto: '{}'. Detalle: {}", MDC.get(MDC_EMAIL_RECIPIENT), MDC.get(MDC_EMAIL_SUBJECT), e.getMessage(), e);
            throw new RuntimeException("Ocurrió un error inesperado al intentar enviar el correo.", e);
        } finally {
            clearNotificacionMDCContext(); // Limpiar MDC al finalizar el método
        }
    }
}