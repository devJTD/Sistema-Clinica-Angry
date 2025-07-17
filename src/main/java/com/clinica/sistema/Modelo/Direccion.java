package com.clinica.sistema.Modelo;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "direcciones")
public class Direccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "La dirección no puede estar vacía.")
    @Size(min = 5, max = 200, message = "La dirección debe tener entre 5 y 200 caracteres.")
    @Column(nullable = false, length = 500)
    private String direccionCompleta;

    @ManyToOne
    @JoinColumn(name = "paciente_id", nullable = false)
    @JsonIgnore
    private Paciente paciente;

    // Constructor vacío
    public Direccion() {
    }

    // Constructor con todos los campos
    public Direccion(Long id, String direccionCompleta, Paciente paciente) {
        this.id = id;
        this.direccionCompleta = direccionCompleta;
        this.paciente = paciente;
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDireccionCompleta() {
        return direccionCompleta;
    }

    public void setDireccionCompleta(String direccionCompleta) {
        this.direccionCompleta = direccionCompleta;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }

    @Override
    public String toString() {
        return "Direccion{" +
                "id=" + id +
                ", direccionCompleta='" + direccionCompleta + '\'' +
                ", paciente=" + (paciente != null ? paciente.getId() : null) +
                '}';
    }
}