package com.clinica.sistema.Modelo;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String mensaje;

    @Column(nullable = false, length = 100)
    private String emailDestinatario;

    @Column(nullable = false)
    private LocalDate fechaEnvio;

    @OneToOne(mappedBy = "notificacion")
    private Cita cita;

    // Constructor vacío
    public Notificacion() {
    }

    // Constructor con todos los campos
    public Notificacion(Long id, String mensaje, String emailDestinatario, LocalDate fechaEnvio, Cita cita) {
        this.id = id;
        this.mensaje = mensaje;
        this.emailDestinatario = emailDestinatario;
        this.fechaEnvio = fechaEnvio;
        this.cita = cita;
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getEmailDestinatario() {
        return emailDestinatario;
    }

    public void setEmailDestinatario(String emailDestinatario) {
        this.emailDestinatario = emailDestinatario;
    }

    public LocalDate getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDate fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public Cita getCita() {
        return cita;
    }

    public void setCita(Cita cita) {
        this.cita = cita;
    }

    @Override
    public String toString() {
        return "Notificacion{" +
                "id=" + id +
                ", mensaje='" + mensaje + '\'' +
                ", emailDestinatario='" + emailDestinatario + '\'' +
                ", fechaEnvio=" + fechaEnvio +
                ", cita=" + (cita != null ? cita.getId() : null) +
                '}';
    }
}
