package com.clinica.sistema.Modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString; // Opcional, pero útil para logs y depuración

@Entity
@Getter // Genera automáticamente todos los getters para los campos de la clase
@Setter // Genera automáticamente todos los setters para los campos de la clase
@NoArgsConstructor // Genera un constructor sin argumentos (equivalente a public Medico() {})
@AllArgsConstructor // Genera un constructor con todos los argumentos (útil para Medico(String nombre, String apellido, Especialidad especialidad))
@ToString // Opcional: Genera un método toString() para facilitar la depuración
public class Medico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String apellido;

    @ManyToOne
    @JoinColumn(name = "especialidad_id")
    private Especialidad especialidad;
}